package com.hospital.appointmentservice.dto;

import java.time.LocalDate;
import java.util.List;

public record MedicalHistoryResponseDTO(
        Long recordId,
        Long patientId,
        Long doctorId,
        String diagnosis,
        LocalDate diagnosedAt,
        List<String> prescribedMeds
) {}
