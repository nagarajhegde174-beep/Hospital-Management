package com.hospital.management.medicalhistoryservice.DTO;

import lombok.Data;

@Data
public class PatientResponseDTO {
    private String name;
    private Long patientId;
    private String responseMessage;
}

