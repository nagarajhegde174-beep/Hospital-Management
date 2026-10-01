package com.hospital.management.patientservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientUpdateDTO {

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^\\d{10}$", message = "Contact details must be 10 digits")
    private String contactDetails;

    private String gender;
}