package com.hospital.appointmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long patientId;
    private Long doctorId;
    private String confirmationCode;  // Public API identifier instead of internal appointmentId
    private String appointmentStatus;            // CONFIRMED, CANCELLED, RESCHEDULED, COMPLETED
    private LocalTime time;           // Appointment time
    private LocalDate date;           // Appointment date
}
