package com.hospital.management.auth_service.client;

import com.hospital.management.auth_service.dto.DoctorRequestDTO;
import com.hospital.management.auth_service.dto.DoctorResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("doctor-profile-service")
public interface DoctorServiceClient {

    @PostMapping("/api/v1/doctors")
    DoctorResponseDTO registerDoctor(@RequestBody DoctorRequestDTO doctorRequestDTO);


    @DeleteMapping("/api/v1/doctors/{id}")          // ← ADD
    void deleteDoctor(@PathVariable("id") Long id);
}