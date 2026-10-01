package com.hospital.management.patientservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/patient/{patientId}/email")  // ← ADD
    String getPatientEmail(@PathVariable("patientId") Long patientId);

    @DeleteMapping("/api/v1/auth/users/{email}")
    void deleteUserByEmail(@PathVariable("email") String email);
}