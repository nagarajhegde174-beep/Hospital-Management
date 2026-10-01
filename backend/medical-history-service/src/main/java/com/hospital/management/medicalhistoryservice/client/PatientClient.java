package com.hospital.management.medicalhistoryservice.client;

import com.hospital.management.medicalhistoryservice.DTO.PatientResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "patient-service")
public interface PatientClient {

    @GetMapping("/api/v1/patients/{patientId}")
    PatientResponseDTO getPatientById(@PathVariable("patientId") Long patientId);
}