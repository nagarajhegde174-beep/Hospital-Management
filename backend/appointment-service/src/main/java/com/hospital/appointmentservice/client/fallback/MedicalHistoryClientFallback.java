package com.hospital.appointmentservice.client.fallback;

import com.hospital.appointmentservice.client.MedicalHistoryClient;
import com.hospital.appointmentservice.dto.MedicalHistoryResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MedicalHistoryClientFallback implements MedicalHistoryClient {

    private static final Logger logger =
            LoggerFactory.getLogger(MedicalHistoryClientFallback.class);

    @Override
    public List<MedicalHistoryResponseDTO> getPatientHistory(Long patientId) {
        logger.warn("FALLBACK: medical-history-service unavailable. " +
                "Returning empty history for patientId: {}", patientId);
        return List.of();
    }
}
