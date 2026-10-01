package com.hospital.appointmentservice.service;

import com.hospital.appointmentservice.client.DoctorServiceClient;
import com.hospital.appointmentservice.client.DoctorSheduleClient;
import com.hospital.appointmentservice.client.MedicalHistoryClient;
import com.hospital.appointmentservice.client.NotificationServiceClient;
import com.hospital.appointmentservice.dto.*;
import com.hospital.appointmentservice.exception.BusinessValidationException;
import com.hospital.appointmentservice.exception.OwnershipException;
import com.hospital.appointmentservice.exception.ResourceNotFoundException;
import com.hospital.appointmentservice.model.Appointment;
import com.hospital.appointmentservice.model.AppointmentStatus;
import com.hospital.appointmentservice.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final DoctorServiceClient doctorServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final DoctorSheduleClient doctorSheduleClient;
    private final MedicalHistoryClient medicalHistoryClient;

    public List<DoctorAvailabilityDTO> getAvailableDoctorsBySpecialization(
            GetAvailableDoctorsRequest request) {
        return doctorServiceClient.getAvailableDoctorsBySpecialization(request.specialization());
    }

    public List<LocalDate> getDoctorAvailableSlots(Long doctorId) {
        return doctorSheduleClient.doctorAvailableSlots(doctorId);
    }

    public List<TimeSlotDTO> getTimeSlotsForDoctor(GetTimeSlotsRequest request) {
        return doctorSheduleClient.getTimeSlotsByDoctorId(request.doctorId(), request.date());
    }

    @Transactional
    public Appointment bookAppointment(BookAppointmentRequest request) {

        if (appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                request.patientId(), request.startTime(), AppointmentStatus.CANCELLED)) {
            throw new BusinessValidationException(
                    "You already have an active appointment scheduled at this time.");
        }

        if (appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusNot(
                request.patientId(), request.doctorId(), request.startTime(),
                AppointmentStatus.CANCELLED)) {
            throw new BusinessValidationException(
                    "You already have an active appointment with this doctor at this specific time.");
        }

        doctorSheduleClient.claimTimeSlot(
                request.doctorId(), request.patientId(), request.startTime());

        Appointment appointment = new Appointment();
        appointment.setPatientId(request.patientId());
        appointment.setDoctorId(request.doctorId());
        appointment.setAppointmentDate(request.startTime());
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        try {
            Appointment savedAppointment = appointmentRepository.save(appointment);
            sendNotificationSafely(savedAppointment, "CONFIRMED");
            return savedAppointment;

        } catch (ObjectOptimisticLockingFailureException e) {
            releaseSlotAfterLocalFailure(
                    request.doctorId(), request.patientId(), request.startTime());
            throw new BusinessValidationException(
                    "Another user just booked this slot. Please try again.");

        } catch (Exception e) {
            releaseSlotAfterLocalFailure(
                    request.doctorId(), request.patientId(), request.startTime());
            throw new RuntimeException(
                    "Failed to confirm appointment due to a system error. Please try again.");
        }
    }

    @Transactional
    public Appointment cancelAppointment(String confirmationCode,
                                         String callerServiceId, String callerRole) {
        Appointment appointment = appointmentRepository
                .findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found with confirmation code: " + confirmationCode));

        if ("ROLE_PATIENT".equals(callerRole)) {
            assertPatientOwnership(appointment.getPatientId(), callerServiceId);
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessValidationException("Appointment is already cancelled.");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessValidationException("Cannot cancel a completed appointment.");
        }

        doctorSheduleClient.cancelBooking(
                appointment.getDoctorId(),
                appointment.getPatientId(),
                appointment.getAppointmentDate());

        appointment.setStatus(AppointmentStatus.CANCELLED);

        try {
            Appointment cancelledAppointment = appointmentRepository.save(appointment);
            sendNotificationSafely(cancelledAppointment, "CANCELLED");
            return cancelledAppointment;

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessValidationException(
                    "Concurrent cancellation detected. Please try again.");
        }
    }

    /**
     * Reschedule — strict one-time per chain:
     *   - Old appointment → CANCELLED (history preserved)
     *   - New appointment → RESCHEDULED (chain ends here)
     *   - Two notifications: CANCELLED (old) + RESCHEDULED (new)
     */
    @Transactional
    public Appointment rescheduleAppointment(RescheduleAppointmentRequest request,
                                              String callerServiceId, String callerRole) {
        Appointment existingAppointment = appointmentRepository
                .findByConfirmationCode(request.confirmationCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found with confirmation code: "
                                + request.confirmationCode()));

        if ("ROLE_PATIENT".equals(callerRole)) {
            assertPatientOwnership(existingAppointment.getPatientId(), callerServiceId);
        }

        if (existingAppointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessValidationException("Cannot reschedule a cancelled appointment.");
        }
        if (existingAppointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessValidationException("Cannot reschedule a completed appointment.");
        }
        if (existingAppointment.getStatus() == AppointmentStatus.RESCHEDULED) {
            throw new BusinessValidationException(
                    "This appointment has already been rescheduled once and cannot be rescheduled again.");
        }

        if (request.newAppointmentTime().equals(existingAppointment.getAppointmentDate())) {
            throw new BusinessValidationException(
                    "New appointment time must differ from the current appointment time.");
        }

        if (appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                existingAppointment.getPatientId(),
                request.newAppointmentTime(),
                AppointmentStatus.CANCELLED)) {
            throw new BusinessValidationException(
                    "You already have an active appointment scheduled at the requested time.");
        }

        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndStatusNot(
                existingAppointment.getDoctorId(),
                request.newAppointmentTime(),
                AppointmentStatus.CANCELLED)) {
            throw new BusinessValidationException(
                    "Doctor is not available at the requested time.");
        }

        doctorSheduleClient.claimTimeSlot(
                existingAppointment.getDoctorId(),
                existingAppointment.getPatientId(),
                request.newAppointmentTime());

        try {
            doctorSheduleClient.cancelBooking(
                    existingAppointment.getDoctorId(),
                    existingAppointment.getPatientId(),
                    existingAppointment.getAppointmentDate());

        } catch (Exception e) {
            logger.warn("Failed to release old slot. Rolling back new slot for: {}",
                    request.confirmationCode());
            try {
                doctorSheduleClient.cancelBooking(
                        existingAppointment.getDoctorId(),
                        existingAppointment.getPatientId(),
                        request.newAppointmentTime());
                logger.info("Rollback successful for confirmationCode: {}",
                        request.confirmationCode());
            } catch (Exception rollbackEx) {
                logger.error("CRITICAL: Rollback failed. Patient {} may hold two remote slots.",
                        existingAppointment.getPatientId(), rollbackEx);
            }
            throw new RuntimeException(
                    "Failed to release old appointment slot. Please try again.");
        }

        try {
            existingAppointment.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(existingAppointment);
            sendNotificationSafely(existingAppointment, "CANCELLED");
        } catch (Exception e) {
            logger.error(
                    "CRITICAL: Failed to cancel old appointment after remote slot swap. " +
                            "ConfirmationCode: {} — manual reconciliation required.",
                    request.confirmationCode(), e);
            throw new RuntimeException("Failed to complete reschedule. Please contact support.");
        }

        Appointment newAppointment = new Appointment();
        newAppointment.setPatientId(existingAppointment.getPatientId());
        newAppointment.setDoctorId(existingAppointment.getDoctorId());
        newAppointment.setAppointmentDate(request.newAppointmentTime());
        newAppointment.setStatus(AppointmentStatus.RESCHEDULED);

        try {
            Appointment savedNewAppointment = appointmentRepository.save(newAppointment);
            sendNotificationSafely(savedNewAppointment, "RESCHEDULED");
            return savedNewAppointment;

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessValidationException(
                    "Concurrent reschedule detected. Please try again.");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to save rescheduled appointment. Please try again.");
        }
    }

    @Transactional
    public Appointment completeAppointment(String confirmationCode,
                                           String callerServiceId, String callerRole) {

        Appointment appointment = appointmentRepository
                .findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found with confirmation code: " + confirmationCode));

        if ("ROLE_DOCTOR".equals(callerRole)) {
            assertDoctorOwnership(appointment.getDoctorId(), callerServiceId);
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessValidationException(
                    "Appointment is already marked as completed.");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessValidationException(
                    "Cannot complete a cancelled appointment.");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);

        Appointment completedAppointment = appointmentRepository.save(appointment);
        sendNotificationSafely(completedAppointment, "COMPLETED");
        return completedAppointment;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> getByPatientId(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patientId);
    }

    public List<Appointment> getByDoctorId(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctorId);
    }

    public List<MedicalHistoryResponseDTO> getPatientHistoryForDoctor(
            String confirmationCode, String callerServiceId, String callerRole) {

        Appointment appointment = appointmentRepository
                .findByConfirmationCode(confirmationCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found with confirmation code: " + confirmationCode));

        if ("ROLE_DOCTOR".equals(callerRole)) {
            assertDoctorOwnership(appointment.getDoctorId(), callerServiceId);
        }

        return medicalHistoryClient.getPatientHistory(appointment.getPatientId());
    }

    // ─── Private helpers (manually logged — AOP does not intercept private methods) ──

    private void releaseSlotAfterLocalFailure(Long doctorId, Long patientId,
                                              java.time.LocalDateTime startTime) {
        try {
            logger.warn("Compensating rollback: releasing remote slot doctorId={} at {}",
                    doctorId, startTime);
            doctorSheduleClient.cancelBooking(doctorId, patientId, startTime);
            logger.info("Compensating rollback successful: slot released doctorId={} at {}",
                    doctorId, startTime);
        } catch (Exception rollbackEx) {
            logger.error(
                    "CRITICAL STATE: Compensating rollback failed. " +
                            "Remote slot for doctorId={} at {} is orphaned. Manual reconciliation required.",
                    doctorId, startTime, rollbackEx);
        }
    }

    private void assertPatientOwnership(Long appointmentPatientId, String callerServiceId) {
        if (callerServiceId == null
                || !callerServiceId.equals(String.valueOf(appointmentPatientId))) {
            throw new OwnershipException(
                    "Access denied: you can only manage your own appointments.");
        }
    }

    private void assertDoctorOwnership(Long appointmentDoctorId, String callerServiceId) {
        if (callerServiceId == null
                || !callerServiceId.equals(String.valueOf(appointmentDoctorId))) {
            throw new OwnershipException(
                    "Access denied: you can only complete your own appointments.");
        }
    }

    private void sendNotificationSafely(Appointment appointment, String actionType) {
        try {
            logger.debug("Sending {} notification for confirmationCode: {}",
                    actionType, appointment.getConfirmationCode());

            NotificationRequest notification = new NotificationRequest();
            notification.setPatientId(appointment.getPatientId());
            notification.setDoctorId(appointment.getDoctorId());
            notification.setConfirmationCode(appointment.getConfirmationCode());
            notification.setAppointmentStatus(appointment.getStatus().toString());
            notification.setTime(appointment.getAppointmentDate().toLocalTime());
            notification.setDate(appointment.getAppointmentDate().toLocalDate());

            notificationServiceClient.sendNotification(notification);
            logger.info("{} notification sent for confirmationCode: {}",
                    actionType, appointment.getConfirmationCode());

        } catch (Exception e) {
            logger.warn("Failed to send {} notification for confirmationCode: {}. Error: {}",
                    actionType, appointment.getConfirmationCode(), e.getMessage());
        }
    }
}