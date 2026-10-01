package com.hospital.management.auth_service.client;

import com.hospital.management.auth_service.dto.PatientRequestDTO;
import com.hospital.management.auth_service.dto.PatientResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("patient-service")
public interface PatientServiceClient {

    @PostMapping("/api/v1/patients")
    PatientResponseDTO registerPatient(@RequestBody PatientRequestDTO patientRequestDTO);

    @DeleteMapping("/api/v1/patients/{patientId}")
    void deletePatient(@PathVariable("patientId") Long patientId);
}