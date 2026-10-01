package com.hospital.appointmentservice.service;

import com.hospital.appointmentservice.client.DoctorSheduleClient;
import com.hospital.appointmentservice.client.NotificationServiceClient;
import com.hospital.appointmentservice.dto.BookAppointmentRequest;
import com.hospital.appointmentservice.dto.RescheduleAppointmentRequest;
import com.hospital.appointmentservice.exception.BusinessValidationException;
import com.hospital.appointmentservice.exception.OwnershipException;
import com.hospital.appointmentservice.model.Appointment;
import com.hospital.appointmentservice.model.AppointmentStatus;
import com.hospital.appointmentservice.repository.AppointmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    private static final Long PATIENT_ID = 101L;
    private static final Long DOCTOR_ID = 201L;
    private static final String CONFIRMATION_CODE = "550e8400-e29b-41d4-a716-446655440000";
    private static final LocalDateTime APPOINTMENT_TIME = LocalDateTime.of(2030, 12, 15, 10, 0);

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorSheduleClient doctorSheduleClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;


    @InjectMocks
    private AppointmentService appointmentService;

    private BookAppointmentRequest createBookingRequest() {
        return new BookAppointmentRequest(PATIENT_ID, DOCTOR_ID, APPOINTMENT_TIME);
    }

    private Appointment createValidAppointment() {
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(1000L);
        appointment.setConfirmationCode(CONFIRMATION_CODE);
        appointment.setPatientId(PATIENT_ID);
        appointment.setDoctorId(DOCTOR_ID);
        appointment.setAppointmentDate(APPOINTMENT_TIME);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointment;
    }

    @Test
    @DisplayName("bookAppointment - Throws Exception when patient already has appointment with doctor at this time")
    void testBookAppointment_AlreadyBooked() {
        BookAppointmentRequest request = createBookingRequest();

        when(appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                request.patientId(), request.startTime(), AppointmentStatus.CANCELLED))
                .thenReturn(false);

        when(appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusNot(
                request.patientId(), request.doctorId(), request.startTime(), AppointmentStatus.CANCELLED))
                .thenReturn(true);

        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> appointmentService.bookAppointment(request));

        assertEquals("You already have an active appointment with this doctor at this specific time.",
                exception.getMessage());

        verify(doctorSheduleClient, never()).claimTimeSlot(anyLong(), anyLong(), any());
        verify(appointmentRepository, never()).save(any());
        verify(notificationServiceClient, never()).sendNotification(any());
    }

    @Test
    @DisplayName("bookAppointment - Handles Optimistic Locking Failure during concurrent booking")
    void testBookAppointment_OptimisticLockingFailure() {
        BookAppointmentRequest request = createBookingRequest();

        when(appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                anyLong(), any(LocalDateTime.class), any(AppointmentStatus.class))).thenReturn(false);
        when(appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusNot(
                anyLong(), anyLong(), any(LocalDateTime.class), any(AppointmentStatus.class))).thenReturn(false);

        doNothing().when(doctorSheduleClient).claimTimeSlot(anyLong(), anyLong(), any());
        doNothing().when(doctorSheduleClient).cancelBooking(anyLong(), anyLong(), any());

        when(appointmentRepository.save(any(Appointment.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Appointment.class, "appointmentId"));

        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> appointmentService.bookAppointment(request));

        assertEquals("Another user just booked this slot. Please try again.", exception.getMessage());

        verify(doctorSheduleClient, times(1)).claimTimeSlot(anyLong(), anyLong(), any());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(notificationServiceClient, never()).sendNotification(any());
    }

    @Test
    @DisplayName("bookAppointment - Succeeds even if Notification Service fails")
    void testBookAppointment_NotificationFails() {
        BookAppointmentRequest request = createBookingRequest();
        Appointment savedAppointment = createValidAppointment();

        when(appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                anyLong(), any(LocalDateTime.class), any(AppointmentStatus.class))).thenReturn(false);
        when(appointmentRepository.existsByPatientIdAndDoctorIdAndAppointmentDateAndStatusNot(
                anyLong(), anyLong(), any(LocalDateTime.class), any(AppointmentStatus.class))).thenReturn(false);

        doNothing().when(doctorSheduleClient).claimTimeSlot(anyLong(), anyLong(), any());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        doThrow(new RuntimeException("Notification server is down!"))
                .when(notificationServiceClient).sendNotification(any());

        Appointment result = appointmentService.bookAppointment(request);

        assertEquals(CONFIRMATION_CODE, result.getConfirmationCode());
        verify(notificationServiceClient, times(1)).sendNotification(any());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("completeAppointment - Doctor can only complete their own appointment (throws OwnershipException → 403)")
    void testCompleteAppointment_DoctorOwnershipEnforced() {
        Appointment existingAppointment = createValidAppointment();

        when(appointmentRepository.findByConfirmationCode(CONFIRMATION_CODE))
                .thenReturn(Optional.of(existingAppointment));

        OwnershipException exception = assertThrows(OwnershipException.class,
                () -> appointmentService.completeAppointment(
                        CONFIRMATION_CODE, "999", "ROLE_DOCTOR"));

        assertEquals("Access denied: you can only complete your own appointments.",
                exception.getMessage());

        verify(appointmentRepository, never()).save(any());
        verify(notificationServiceClient, never()).sendNotification(any());
    }

    @Test
    @DisplayName("completeAppointment - Success when doctor owns the appointment")
    void testCompleteAppointment_Success() {
        Appointment existingAppointment = createValidAppointment();

        when(appointmentRepository.findByConfirmationCode(CONFIRMATION_CODE))
                .thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.save(any(Appointment.class))).then(returnsFirstArg());
        doNothing().when(notificationServiceClient).sendNotification(any());

        Appointment result = appointmentService.completeAppointment(
                CONFIRMATION_CODE, String.valueOf(DOCTOR_ID), "ROLE_DOCTOR");

        assertEquals(AppointmentStatus.COMPLETED, result.getStatus());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(notificationServiceClient, times(1)).sendNotification(any());
    }

    @Test
    @DisplayName("cancelAppointment - Patient can only cancel their own appointment (throws OwnershipException → 403)")
    void testCancelAppointment_byDifferentPatient_throwsOwnershipException() {
        Appointment existingAppointment = createValidAppointment(); // patientId = 101L

        when(appointmentRepository.findByConfirmationCode(CONFIRMATION_CODE))
                .thenReturn(Optional.of(existingAppointment));

        OwnershipException exception = assertThrows(OwnershipException.class,
                () -> appointmentService.cancelAppointment(CONFIRMATION_CODE, "999", "ROLE_PATIENT"));

        assertEquals("Access denied: you can only manage your own appointments.",
                exception.getMessage());

        verify(appointmentRepository, never()).save(any());
        verify(notificationServiceClient, never()).sendNotification(any());
    }

    @Test
    @DisplayName("completeAppointment - Different doctor cannot complete appointment (throws OwnershipException → 403)")
    void testCompleteAppointment_byDifferentDoctor_throwsOwnershipException() {
        Appointment existingAppointment = createValidAppointment(); // doctorId = 201L

        when(appointmentRepository.findByConfirmationCode(CONFIRMATION_CODE))
                .thenReturn(Optional.of(existingAppointment));

        OwnershipException exception = assertThrows(OwnershipException.class,
                () -> appointmentService.completeAppointment(CONFIRMATION_CODE, "999", "ROLE_DOCTOR"));

        assertEquals("Access denied: you can only complete your own appointments.",
                exception.getMessage());

        verify(appointmentRepository, never()).save(any());
        verify(notificationServiceClient, never()).sendNotification(any());
    }

    @Test
    @DisplayName("cancelAppointment - Successfully cancels appointment and sends notification")
    void testCancelAppointment_Success() {
        Appointment existingAppointment = createValidAppointment();

        when(appointmentRepository.findByConfirmationCode(CONFIRMATION_CODE))
                .thenReturn(Optional.of(existingAppointment));
        doNothing().when(doctorSheduleClient).cancelBooking(anyLong(), anyLong(), any(LocalDateTime.class));
        when(appointmentRepository.save(any(Appointment.class))).then(returnsFirstArg());
        doNothing().when(notificationServiceClient).sendNotification(any());

        Appointment result = appointmentService.cancelAppointment(CONFIRMATION_CODE, null, "ROLE_ADMIN");

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertEquals(CONFIRMATION_CODE, result.getConfirmationCode());

        verify(appointmentRepository, times(1)).findByConfirmationCode(CONFIRMATION_CODE);
        verify(doctorSheduleClient, times(1)).cancelBooking(DOCTOR_ID, PATIENT_ID, APPOINTMENT_TIME);
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(notificationServiceClient, times(1)).sendNotification(any());
    }

    @Test
    @DisplayName("rescheduleAppointment - Successfully reschedules and sends notification")
    void testRescheduleAppointment_Success() {
        LocalDateTime newTime = APPOINTMENT_TIME.plusDays(5);
        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest(CONFIRMATION_CODE, newTime);
        Appointment existingAppointment = createValidAppointment();

        when(appointmentRepository.findByConfirmationCode(CONFIRMATION_CODE))
                .thenReturn(Optional.of(existingAppointment));

        when(appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                anyLong(), any(LocalDateTime.class), any(AppointmentStatus.class))).thenReturn(false);
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndStatusNot(
                DOCTOR_ID, newTime, AppointmentStatus.CANCELLED)).thenReturn(false);

        doNothing().when(doctorSheduleClient).claimTimeSlot(DOCTOR_ID, PATIENT_ID, newTime);
        doNothing().when(doctorSheduleClient).cancelBooking(DOCTOR_ID, PATIENT_ID, APPOINTMENT_TIME);
        when(appointmentRepository.save(any(Appointment.class))).then(returnsFirstArg());
        doNothing().when(notificationServiceClient).sendNotification(any());

        Appointment result = appointmentService.rescheduleAppointment(request, null, "ROLE_ADMIN");

        assertEquals(newTime, result.getAppointmentDate());

        assertEquals(AppointmentStatus.RESCHEDULED, result.getStatus());

        verify(doctorSheduleClient, times(1)).claimTimeSlot(DOCTOR_ID, PATIENT_ID, newTime);
        verify(doctorSheduleClient, times(1)).cancelBooking(DOCTOR_ID, PATIENT_ID, APPOINTMENT_TIME);
        verify(appointmentRepository, times(2)).save(any(Appointment.class));
        verify(notificationServiceClient, times(2)).sendNotification(any());
    }
}