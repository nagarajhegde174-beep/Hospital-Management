package com.notification.management.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class NotificationRequestDTO {
    private Long patientId;
    private Long doctorId;
    private String confirmationCode;
    private AppointmentStatus appointmentStatus;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime time;
}