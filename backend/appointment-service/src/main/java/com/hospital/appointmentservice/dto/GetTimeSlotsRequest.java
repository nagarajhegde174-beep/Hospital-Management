package com.hospital.appointmentservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GetTimeSlotsRequest(
        @NotNull(message = "Patient ID is required")
        Long patientId,

        @NotNull(message = "Doctor ID is required")
        Long doctorId,

        @NotNull(message = "date is required")
        LocalDate date
) {}
