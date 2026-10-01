package com.hospital.management.medicalhistoryservice.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MedicalHistoryResponseDTO {
    private Long recordId;
    private String diagnosis;
    private LocalDate diagnosedAt;
    private Long patientId;
    private Long doctorId;
    private List<String> prescribedMeds;
}
