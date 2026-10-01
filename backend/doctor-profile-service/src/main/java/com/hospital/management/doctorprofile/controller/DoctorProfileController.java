package com.hospital.management.doctorprofile.controller;

import com.hospital.management.doctorprofile.dto.DoctorAppointmentResponseDTO;
import com.hospital.management.doctorprofile.dto.DoctorRegistrationDTO;
import com.hospital.management.doctorprofile.dto.DoctorProfileResponseDTO;
import com.hospital.management.doctorprofile.dto.DoctorUpdateDTO;
import com.hospital.management.doctorprofile.exception.DoctorNotFoundException;
import com.hospital.management.doctorprofile.service.DoctorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorProfileController {


    private final DoctorProfileService doctorProfileService;

    @PostMapping
    public ResponseEntity<DoctorProfileResponseDTO> registerDoctor(
            @Valid @RequestBody DoctorRegistrationDTO requestDTO) {
        log.info("Register doctor request received");
        DoctorProfileResponseDTO response = doctorProfileService.registerDoctor(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorProfileResponseDTO> getDoctorById(@PathVariable Long id) {
        log.info("Get doctor by id={}", id);
        return ResponseEntity.ok(doctorProfileService.getDoctorById(id));
    }

    @GetMapping
    public ResponseEntity<List<DoctorProfileResponseDTO>> getAllDoctors() {
        log.info("Get all doctors request received");
        return ResponseEntity.ok(doctorProfileService.getAllDoctors());
    }

    @GetMapping("/specialization/{specialization}")
    public ResponseEntity<List<DoctorAppointmentResponseDTO>> getDoctorsBySpecialization(
            @PathVariable String specialization) {
        log.info("Search doctors by specialization={}", specialization);
        return ResponseEntity.ok(doctorProfileService.getDoctorsBySpecialization(specialization));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DoctorProfileResponseDTO> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorUpdateDTO updateDTO) {
        log.info("Update doctor request for id={}", id);
        return ResponseEntity.ok(doctorProfileService.updateDoctor(id, updateDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDoctorById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        log.info("Delete doctor by id={}", id);
        if (role != null && !"ROLE_ADMIN".equals(role)) {
            throw new DoctorNotFoundException("Access denied: only admins can delete doctors.", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(doctorProfileService.deleteDoctorById(id));
    }

}