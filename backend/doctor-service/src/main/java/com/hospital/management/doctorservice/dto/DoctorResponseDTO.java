package com.hospital.management.doctorservice.dto;

import lombok.Data;

@Data
public class DoctorResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String specialization;
    private String designation;
}