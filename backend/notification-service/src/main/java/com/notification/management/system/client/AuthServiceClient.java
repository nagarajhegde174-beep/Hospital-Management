package com.notification.management.system.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/patient/{patientId}/email")
    String getPatientEmail(@PathVariable Long patientId);

    @GetMapping("/api/v1/auth/doctor/{doctorId}/email")
    String getDoctorEmail(@PathVariable Long doctorId);
}