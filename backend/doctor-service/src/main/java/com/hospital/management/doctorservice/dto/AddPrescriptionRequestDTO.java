package com.hospital.management.doctorservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AddPrescriptionRequestDTO {

    @NotBlank(message = "Diagnosis cannot be blank")
    private String diagnosis;

    @NotEmpty(message = "At least one medication must be prescribed")
    private List<String> prescribedMeds;

    @PastOrPresent(message = "Visit date cannot be a future date")
    private LocalDate visitDate;
}