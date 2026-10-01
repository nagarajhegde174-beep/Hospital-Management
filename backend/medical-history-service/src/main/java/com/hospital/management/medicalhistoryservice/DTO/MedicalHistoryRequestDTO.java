package com.hospital.management.medicalhistoryservice.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MedicalHistoryRequestDTO {
    @NotBlank(message = "Diagnosis cannot be blank")
    @Size(max = 100, message = "Diagnosis cannot exceed 100 characters")
    private String diagnosis;
    private LocalDate diagnosedAt;
    @NotNull(message="Patient Id cannot be null")
    private Long patientId;
    private Long doctorId;
    private List<String> prescribedMeds;
}
