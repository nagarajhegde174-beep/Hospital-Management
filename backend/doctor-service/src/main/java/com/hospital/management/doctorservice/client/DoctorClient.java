package com.hospital.management.doctorservice.client;

import com.hospital.management.doctorservice.dto.DoctorResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "doctor-profile-service")
public interface DoctorClient {

    @GetMapping("/api/v1/doctors/{id}")
    DoctorResponseDTO getDoctorById(@PathVariable("id") Long id);
}
