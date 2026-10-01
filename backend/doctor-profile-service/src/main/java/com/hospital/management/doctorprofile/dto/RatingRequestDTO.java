package com.hospital.management.doctorprofile.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingRequestDTO {

    @NotNull
    private Long doctorId;

    @NotNull
    private Long patientId;

    @NotNull
    private Long appointmentId;

    @Min(1) @Max(5)
    private int rating;

    @Size(max = 500)
    private String comment;
}