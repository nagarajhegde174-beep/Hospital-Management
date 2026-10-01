package com.hospital.appointmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GetAvailableDoctorsRequest(
        @NotNull(message = "Patient ID is required")
        Long patientId,

        @NotBlank(message = "Specialization is required")
        String specialization
) {}
