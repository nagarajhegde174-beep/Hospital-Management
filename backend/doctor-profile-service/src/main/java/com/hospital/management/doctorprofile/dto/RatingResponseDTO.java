package com.hospital.management.doctorprofile.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponseDTO {
    private Long id;
    private Long doctorId;
    private Long patientId;
    private Long appointmentId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}