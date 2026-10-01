package com.notification.management.system.client;

import com.notification.management.system.dto.PatientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "patient-service")
public interface PatientServiceClient {

    @GetMapping("/api/v1/patients/{patientId}")
    PatientResponse getPatientById(@PathVariable Long patientId);
}