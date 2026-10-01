package com.hospital.management.doctorprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DoctorAppointmentResponseDTO {
    private Long doctorId;
    private String doctorName;
    private String specialization;
    private String designation;
}