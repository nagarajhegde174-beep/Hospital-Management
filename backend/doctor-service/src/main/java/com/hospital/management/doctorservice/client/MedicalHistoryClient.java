package com.hospital.management.doctorservice.client;

import com.hospital.management.doctorservice.dto.MedicalHistoryRequestDTO;
import com.hospital.management.doctorservice.dto.MedicalHistoryResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "medical-history-service")
public interface MedicalHistoryClient {

    @GetMapping("/api/v1/patient/{patientId}")
    List<MedicalHistoryResponseDTO> getPatientHistory(
            @PathVariable("patientId") Long patientId);

    @PostMapping("/api/v1")
    MedicalHistoryResponseDTO addPrescription(@RequestBody MedicalHistoryRequestDTO request);
}