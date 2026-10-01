package com.hospital.appointmentservice.client.fallback;

import com.hospital.appointmentservice.client.DoctorServiceClient;
import com.hospital.appointmentservice.dto.DoctorAvailabilityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;


@Component
public class DoctorServiceClientFallback implements DoctorServiceClient {

    private static final Logger logger =
            LoggerFactory.getLogger(DoctorServiceClientFallback.class);

    @Override
    public List<DoctorAvailabilityDTO> getAvailableDoctorsBySpecialization(String specialization) {
        logger.warn("FALLBACK: doctor-profile-service unavailable. " +
                "Returning empty doctor list for specialization: {}", specialization);
        return List.of();
    }
}
