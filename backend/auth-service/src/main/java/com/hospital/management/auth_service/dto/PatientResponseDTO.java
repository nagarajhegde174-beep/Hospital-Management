package com.hospital.management.auth_service.dto;


import lombok.Data;

@Data
public class PatientResponseDTO {
    private Long patientId;  // must match the field name in patient-service's response
}