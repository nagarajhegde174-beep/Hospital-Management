package com.hospital.appointmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;

public record RescheduleAppointmentRequest(
        @NotBlank(message = "Confirmation code is required")
        String confirmationCode,

        @NotNull(message = "New appointment time is required")
        @Future(message = "New appointment time must be in the future")
        LocalDateTime newAppointmentTime
) {}
