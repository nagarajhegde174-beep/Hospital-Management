package com.hospital.management.doctorprofile.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// New file: AuthServiceClient.java
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/doctor/{doctorId}/email")   // ← ADD
    String getDoctorEmail(@PathVariable("doctorId") Long doctorId);

    @DeleteMapping("/api/v1/auth/users/{email}")
    void deleteUserByEmail(@PathVariable String email);
}