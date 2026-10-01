package com.hospital.management.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicalHistoryRequestDTO {
    private String diagnosis;
    private LocalDate diagnosedAt;
    private Long patientId;
    private Long doctorId;
    private List<String> prescribedMeds;
}
