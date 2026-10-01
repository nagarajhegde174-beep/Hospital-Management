package com.hospital.management.patientservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicalHistoryResponseDTO {
    private String diagnosis;
    private List<String> prescribedMeds;
    private LocalDate visitDate;
    private Long patientId;
}