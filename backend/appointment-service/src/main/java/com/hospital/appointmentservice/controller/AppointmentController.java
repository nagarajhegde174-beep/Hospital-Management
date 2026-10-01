package com.hospital.appointmentservice.controller;

import com.hospital.appointmentservice.model.Appointment;
import com.hospital.appointmentservice.dto.*;
import com.hospital.appointmentservice.exception.OwnershipException;
import com.hospital.appointmentservice.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "AppointmentController", description = "The methods are used for managing appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "Get the Available Doctors",
            description = "Patient can get the available doctors with specialization")
    @PostMapping("/available-doctors")
    public ResponseEntity<List<DoctorAvailabilityDTO>> getAvailableDoctors(
            @Valid @RequestBody GetAvailableDoctorsRequest request) {
        List<DoctorAvailabilityDTO> doctors =
                appointmentService.getAvailableDoctorsBySpecialization(request);
        return ResponseEntity.ok(doctors);
    }

    @Operation(summary = "List appointments for a patient or doctor",
            description = "Pass patientId OR doctorId as a query param")
    @GetMapping
    public ResponseEntity<List<Appointment>> getAppointments(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        if (patientId != null) {
            if ("ROLE_PATIENT".equals(role)) {
                assertOwnership(patientId, serviceId);
            }
            return ResponseEntity.ok(appointmentService.getByPatientId(patientId));
        }
        if (doctorId != null) {
            if ("ROLE_PATIENT".equals(role)) {
                throw new OwnershipException("Access denied: patients cannot query by doctor ID.");
            }
            if ("ROLE_DOCTOR".equals(role)) {
                assertOwnership(doctorId, serviceId);
            }
            return ResponseEntity.ok(appointmentService.getByDoctorId(doctorId));
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Get all appointments (admin only)")
    @GetMapping("/all")
    public ResponseEntity<List<Appointment>> getAllAppointments(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @Operation(summary = "Get Available Dates of a Doctor",
            description = "Patient can see all available dates/time slots for a selected doctor")
    @GetMapping("/available-dates/{doctorId}")
    public ResponseEntity<List<LocalDate>> getAvailableDates(
            @PathVariable("doctorId") Long doctorId) {
        List<LocalDate> slots = appointmentService.getDoctorAvailableSlots(doctorId);
        return ResponseEntity.ok(slots);
    }

    @Operation(summary = "Get Available slots of selected Doctor",
            description = "Patient can get the available slots of a selected doctor")
    @PostMapping("/available-slots")
    public ResponseEntity<List<TimeSlotDTO>> getAvailableSlots(
            @Valid @RequestBody GetTimeSlotsRequest request) {
        List<TimeSlotDTO> slots = appointmentService.getTimeSlotsForDoctor(request);
        return ResponseEntity.ok(slots);
    }

    @Operation(summary = "Book an Appointment",
            description = "Patient can book an appointment with a selected doctor")
    @PostMapping("/booking")
    public ResponseEntity<Appointment> bookAppointment(
            @Valid @RequestBody BookAppointmentRequest request,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if ("ROLE_PATIENT".equals(role)) {
            assertOwnership(request.patientId(), serviceId);
        }
        Appointment appointment = appointmentService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    @Operation(summary = "Cancel an Appointment",
            description = "Patient can cancel an appointment")
    @PatchMapping("/cancel/{confirmationCode}")
    public ResponseEntity<Appointment> cancelAppointment(
            @PathVariable("confirmationCode") String confirmationCode,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        Appointment appointment = appointmentService.cancelAppointment(
                confirmationCode, serviceId, role);
        return ResponseEntity.ok(appointment);
    }

    @Operation(summary = "Reschedule an Appointment",
            description = "Patient can reschedule an appointment")
    @PatchMapping("/reschedule")
    public ResponseEntity<Appointment> rescheduleAppointment(
            @Valid @RequestBody RescheduleAppointmentRequest request,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        Appointment result = appointmentService.rescheduleAppointment(
                request, serviceId, role);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Complete an Appointment",
            description = "Doctor marks a confirmed appointment as completed after seeing the patient")
    @PatchMapping("/complete/{confirmationCode}")
    public ResponseEntity<Appointment> completeAppointment(
            @PathVariable("confirmationCode") String confirmationCode,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        Appointment appointment = appointmentService.completeAppointment(
                confirmationCode, serviceId, role);
        return ResponseEntity.ok(appointment);
    }

    @Operation(summary = "Get patient medical history (doctor only)",
            description = "Doctor retrieves the full medical history of the patient " +
                    "linked to a given appointment confirmation code")
    @GetMapping("/{confirmationCode}/patient-history")
    public ResponseEntity<List<MedicalHistoryResponseDTO>> getPatientHistory(
            @PathVariable("confirmationCode") String confirmationCode,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        List<MedicalHistoryResponseDTO> history =
                appointmentService.getPatientHistoryForDoctor(confirmationCode, serviceId, role);
        return ResponseEntity.ok(history);
    }

    private void assertOwnership(Long patientId, String serviceId) {
        if (serviceId == null || !serviceId.equals(String.valueOf(patientId))) {
            throw new OwnershipException(
                    "Access denied: you can only manage your own appointments.");
        }
    }
}