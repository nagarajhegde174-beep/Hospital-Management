package com.hospital.management.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRequestDTO {

    private String name;
    private String email;
    private String contactDetails;
    private String specialization;
    private String designation;
}