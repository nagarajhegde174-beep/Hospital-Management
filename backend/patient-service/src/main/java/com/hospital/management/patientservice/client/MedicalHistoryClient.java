package com.hospital.management.patientservice.client;


import com.hospital.management.patientservice.dto.MedicalHistoryResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;


@FeignClient(name = "medical-history-service")
public interface MedicalHistoryClient {

    @GetMapping("/api/v1/patient/{patientId}")
    List<MedicalHistoryResponseDTO> getPatientHistory(@PathVariable("patientId") Long patientId);
}