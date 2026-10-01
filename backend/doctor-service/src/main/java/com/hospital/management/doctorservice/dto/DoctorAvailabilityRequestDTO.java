package com.hospital.management.doctorservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;


@Data
public class DoctorAvailabilityRequestDTO {

    @NotNull(message = "Doctor ID must not be null")
    private Long doctorId;

    @NotNull(message = "Date must not be null")
    @Future(message = "Availability date must be in the future")
    private LocalDate date;

    @NotNull(message = "Shift start time must not be null")
    private LocalTime shiftStart;

    @NotNull(message = "Shift end time must not be null")
    private LocalTime shiftEnd;

    private LocalTime breakStart;
}