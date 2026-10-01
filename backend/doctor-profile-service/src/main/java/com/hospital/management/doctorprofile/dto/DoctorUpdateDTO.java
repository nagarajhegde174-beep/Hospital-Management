package com.hospital.management.doctorprofile.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;


@Data
public class DoctorUpdateDTO {

    // Optional — only update if provided
    private String name;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone Number must be a 10-digit number")
    private String contactDetails;

    private String specialization;

    private String designation;
}

