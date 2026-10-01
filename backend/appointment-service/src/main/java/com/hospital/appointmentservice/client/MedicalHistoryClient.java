package com.hospital.appointmentservice.client;

import com.hospital.appointmentservice.client.fallback.MedicalHistoryClientFallback;
import com.hospital.appointmentservice.dto.MedicalHistoryResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "medical-history-service",
        fallback = MedicalHistoryClientFallback.class)
public interface MedicalHistoryClient {

    @GetMapping("/api/v1/patient/{patientId}")
    List<MedicalHistoryResponseDTO> getPatientHistory(
            @PathVariable("patientId") Long patientId);
}
