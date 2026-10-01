package com.hospital.management.medicalhistoryservice.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MedicalHistoryUpdateDTO {
    @NotNull
    private String diagnosis;
    private List<String> prescribedMeds;
}
