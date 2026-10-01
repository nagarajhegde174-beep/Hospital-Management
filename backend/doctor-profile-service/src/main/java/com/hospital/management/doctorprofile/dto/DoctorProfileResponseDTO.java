package com.hospital.management.doctorprofile.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorProfileResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String contactDetails;
    private String specialization;
    private String designation;
}