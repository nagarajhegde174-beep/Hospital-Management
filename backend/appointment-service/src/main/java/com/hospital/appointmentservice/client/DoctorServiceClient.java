package com.hospital.appointmentservice.client;

import com.hospital.appointmentservice.client.fallback.DoctorServiceClientFallback;
import com.hospital.appointmentservice.dto.DoctorAvailabilityDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "doctor-profile-service",
        fallback = DoctorServiceClientFallback.class)
public interface DoctorServiceClient {

    @GetMapping("/api/v1/doctors/specialization/{specialization}")
    List<DoctorAvailabilityDTO> getAvailableDoctorsBySpecialization(

            @PathVariable("specialization") String specialization
    );
}