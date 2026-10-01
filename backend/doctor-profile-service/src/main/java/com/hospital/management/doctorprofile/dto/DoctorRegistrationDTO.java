package com.hospital.management.doctorprofile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


@Data
public class DoctorRegistrationDTO {

    @NotBlank(message = "Name must not be blank")
    private String name;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Phone Number must not be blank")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone Number must be a 10-digit number")
    private String contactDetails;

    @NotBlank(message = "Specialization must not be blank")
    private String specialization;

    @NotBlank(message = "Designation must not be blank")
    private String designation;
}