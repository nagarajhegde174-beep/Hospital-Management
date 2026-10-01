package com.hospital.management.medicalhistoryservice.controller;

import com.hospital.management.medicalhistoryservice.DTO.MedicalHistoryRequestDTO;
import com.hospital.management.medicalhistoryservice.DTO.MedicalHistoryResponseDTO;
import com.hospital.management.medicalhistoryservice.DTO.Mapperdto;
import com.hospital.management.medicalhistoryservice.DTO.MedicalHistoryUpdateDTO;
import com.hospital.management.medicalhistoryservice.model.MedicalHistory;
import com.hospital.management.medicalhistoryservice.service.MedicalHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MedicalHistoryController{

    private final MedicalHistoryService medicalHistoryService;
    private final Mapperdto mapper;

    @PostMapping
    public ResponseEntity<MedicalHistoryResponseDTO> addMedicalHistory(
            @Valid @RequestBody MedicalHistoryRequestDTO dto,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId) {
        if ("ROLE_PATIENT".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: patients cannot create medical records.");
        }
        if (dto.getDoctorId() == null && serviceId != null && !serviceId.isBlank()) {
            try { dto.setDoctorId(Long.parseLong(serviceId)); } catch (NumberFormatException ignored) {}
        }
        return new ResponseEntity<>(mapper.mapToResponse(medicalHistoryService.addMedicalHistory(dto)),
                HttpStatus.CREATED);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<MedicalHistoryResponseDTO> getMedicalRecord(
            @PathVariable("recordId") long recordId,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        MedicalHistory record = medicalHistoryService.getMedicalHistoryById(recordId);
        if ("ROLE_PATIENT".equals(role)) {
            assertPatientOwnership(serviceId, record.getPatientId());
        }
        return new ResponseEntity<>(mapper.mapToResponse(record), HttpStatus.OK);
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<MedicalHistoryResponseDTO> updateMedicalHistory(
            @PathVariable Long recordId,
            @Valid @RequestBody MedicalHistoryUpdateDTO medicalHistoryRequestDTO,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if ("ROLE_PATIENT".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: patients cannot modify medical records.");
        }
        return new ResponseEntity<>(mapper.mapToResponse(medicalHistoryService.updateMedicalHistory(recordId, medicalHistoryRequestDTO)), HttpStatus.OK);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalHistoryResponseDTO>> getPatientHistory(
            @PathVariable("patientId") long patientId,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if ("ROLE_PATIENT".equals(role)) {
            assertPatientOwnership(serviceId, patientId);
        }
        return new ResponseEntity<>(
                mapper.mapToResponse(medicalHistoryService.getMedicalHistoryByPatientId(patientId)),
                HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<Page<MedicalHistoryResponseDTO>> getAllMedicalHistories(@RequestParam Integer pageNumber,
                                                                                  @RequestParam Integer pageSize,
                                                                                  @RequestParam String order,
                                                                                  @RequestParam String columnName){
        return new ResponseEntity<>(mapper.mapToResponsePage(medicalHistoryService.getAllMedicalHistories(pageNumber,pageSize,order,columnName)),HttpStatus.OK);
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteMedicalHistory(
            @PathVariable("recordId") Long recordId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: only admins can delete medical records.");
        }
        medicalHistoryService.deleteMedicalHistory(recordId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/patient/{patientId}/paged")
    public ResponseEntity<Page<MedicalHistoryResponseDTO>> getPatientHistoryByPage(
            @PathVariable Long patientId,
            @RequestParam Integer pageNumber,
            @RequestParam Integer pageSize,
            @RequestParam String order,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if ("ROLE_PATIENT".equals(role)) {
            assertPatientOwnership(serviceId, patientId);
        }
        return new ResponseEntity<>(
                mapper.mapToResponsePage(medicalHistoryService.getMedicalHistoryByPatientId(
                        patientId, pageNumber, pageSize, order)),
                HttpStatus.OK);
    }

    private void assertPatientOwnership(String serviceId, long patientId) {
        if (serviceId == null || !serviceId.equals(String.valueOf(patientId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: you can only view your own medical history.");
        }
    }
}