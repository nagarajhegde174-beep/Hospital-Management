package com.notification.management.system.client;

import com.notification.management.system.dto.DoctorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "doctor-profile-service")
public interface DoctorServiceClient {

    @GetMapping("/api/v1/doctors/{doctorId}")
    DoctorResponse getDoctorById(@PathVariable Long doctorId);
}