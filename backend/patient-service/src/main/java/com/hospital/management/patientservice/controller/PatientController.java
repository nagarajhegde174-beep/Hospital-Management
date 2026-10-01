package com.hospital.management.patientservice.controller;


import com.hospital.management.patientservice.dto.PatientRegistrationDTO;
import com.hospital.management.patientservice.dto.PatientResponseDTO;
import com.hospital.management.patientservice.dto.PatientUpdateDTO;
import com.hospital.management.patientservice.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    public ResponseEntity<PatientResponseDTO> registerPatient(
            @Valid @RequestBody PatientRegistrationDTO dto) {
        return new ResponseEntity<>(
                patientService.registerPatient(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<PatientResponseDTO> getPatientById(
            @PathVariable long patientId,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        assertPatientOwnership(patientId, serviceId, role);
        return ResponseEntity.ok(patientService.getPatientById(patientId));
    }

    @PatchMapping("/{patientId}")
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable long patientId,
            @Valid @RequestBody PatientUpdateDTO dto,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        assertPatientOwnership(patientId, serviceId, role);
        return ResponseEntity.ok(patientService.updatePatientById(patientId, dto));
    }

    @GetMapping
    public ResponseEntity<List<PatientResponseDTO>> getAllPatients(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: only admins can list all patients.");
        }
        return ResponseEntity.ok(patientService.getAllPatient());
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<String> deletePatientById(
            @PathVariable long patientId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: only admins can delete patients.");
        }
        return ResponseEntity.ok(patientService.deletePatientById(patientId));
    }

    private void assertPatientOwnership(long patientId, String serviceId, String role) {
        if ("ROLE_PATIENT".equals(role)) {
            if (serviceId == null || !serviceId.equals(String.valueOf(patientId))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: you can only access your own patient record.");
            }
        }
    }
}