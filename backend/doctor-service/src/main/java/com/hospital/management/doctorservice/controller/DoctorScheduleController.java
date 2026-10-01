package com.hospital.management.doctorservice.controller;

import com.hospital.management.doctorservice.dto.AddPrescriptionRequestDTO;
import com.hospital.management.doctorservice.dto.DoctorAvailabilityRequestDTO;
import com.hospital.management.doctorservice.dto.MedicalHistoryResponseDTO;
import com.hospital.management.doctorservice.dto.TimeSlotDTO;
import com.hospital.management.doctorservice.service.DoctorScheduleInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Tag(name = "Doctor Schedule API", description = "Manages doctor availability and slot booking")
@RestController
@RequestMapping("/api/v1/doctor-schedule")
@RequiredArgsConstructor
public class DoctorScheduleController {


    private final DoctorScheduleInterface scheduleService;

    @Operation(summary = "Set doctor availability", description = "Creates shift and generates 30-min slots. Locks after save.")
    @PostMapping("/set-availability")
    public ResponseEntity<String> setAvailability(
            @Valid @RequestBody DoctorAvailabilityRequestDTO availabilityDTO) {

        log.info("Received set-availability request for doctorId={}",
                availabilityDTO.getDoctorId());
        String response = scheduleService.createMonthlySchedule(availabilityDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Doctor Timeslots", description = "Returns all the time slots available for a particular doctor based on availability")
    @GetMapping("/slots/doctor/{doctorId}/{date}")
    public ResponseEntity<List<TimeSlotDTO>> getTimeSlotsByDoctorIdandDate(
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Fetching slots for doctorId={} on date={}", doctorId, date);
        return ResponseEntity.ok(scheduleService.getTimeSlotsByDoctorId(doctorId, date));
    }

    @Operation(summary = "Update the Slot", description = "Update the slots if the slots are booked by patient, and marked true")
    @PutMapping("/slots/claim")
    public ResponseEntity<Void> claimTimeSlot(
            @RequestParam Long doctorId,
            @RequestParam Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime) {

        log.info("Claim slot request — doctorId={}, patientId={}, startTime={}",
                doctorId, patientId, startTime);
        scheduleService.claimTimeSlot(doctorId, patientId, startTime);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete the booked Timeslot", description = "Patient can able to delete/cancel their booked timeslot")
    @PutMapping("/cancel-booking")
    public ResponseEntity<String> cancelBooking(
            @RequestParam Long doctorId,
            @RequestParam Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime) {

        log.info("Cancel booking — doctorId={}, patientId={}, startTime={}",
                doctorId, patientId, startTime);
        scheduleService.cancelSlotBooking(doctorId, patientId, startTime);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "View the patient medical history", description = "Doctor can view the patient history based on their previous visit")
    @GetMapping("/view-history/{slotId}")
    public ResponseEntity<List<MedicalHistoryResponseDTO>> viewPatientHistory(
            @PathVariable Long slotId) {

        log.info("View history request for slotId={}", slotId);
        List<MedicalHistoryResponseDTO> filteredHistory =
                scheduleService.getPatientHistoryForDoctor(slotId);
        return ResponseEntity.ok(filteredHistory);
    }

    @Operation(summary = "Get doctor availability dates", description = "Patient can view the doctor availability dates")
    @GetMapping("/available-dates/{doctorId}")
    public ResponseEntity<List<LocalDate>> getAvailableDates(
            @PathVariable Long doctorId) {

        log.info("Available dates request for doctorId={}", doctorId);
        return ResponseEntity.ok(scheduleService.getAvailableDatesForDoctor(doctorId));
    }

    @Operation(summary = "Add prescription after consultation", description = "Doctor adds diagnosis and prescribed medications for a booked slot")
    @PostMapping("/add-prescription/{slotId}")
    public ResponseEntity<MedicalHistoryResponseDTO> addPrescription(
            @PathVariable Long slotId,
            @Valid @RequestBody AddPrescriptionRequestDTO requestDTO) {

        log.info("Add prescription request for slotId={}", slotId);
        MedicalHistoryResponseDTO saved = scheduleService.addPrescription(slotId, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}