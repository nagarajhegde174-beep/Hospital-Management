package com.hospital.appointmentservice.client.fallback;

import com.hospital.appointmentservice.client.DoctorSheduleClient;
import com.hospital.appointmentservice.dto.TimeSlotDTO;
import com.hospital.appointmentservice.exception.BusinessValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class DoctorScheduleClientFallbackFactory implements FallbackFactory<DoctorSheduleClient> {

    private static final Logger logger =
            LoggerFactory.getLogger(DoctorScheduleClientFallbackFactory.class);


    private static final Pattern JSON_MESSAGE =
            Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");

    private String extractMessage(Throwable cause, String fallbackMessage) {
        if (cause == null || cause.getMessage() == null) {
            return fallbackMessage;
        }
        Matcher m = JSON_MESSAGE.matcher(cause.getMessage());
        return m.find() ? m.group(1) : fallbackMessage;
    }

    @Override
    public DoctorSheduleClient create(Throwable cause) {
        return new DoctorSheduleClient() {


            @Override
            public List<LocalDate> doctorAvailableSlots(Long doctorId) {
                logger.warn("FALLBACK: doctorAvailableSlots failed. doctorId={}. Cause: {}",
                        doctorId, cause.getMessage());
                return List.of();
            }

            @Override
            public List<TimeSlotDTO> getTimeSlotsByDoctorId(Long doctorId, LocalDate date) {
                logger.warn("FALLBACK: getTimeSlots failed. doctorId={}, date={}. Cause: {}",
                        doctorId, date, cause.getMessage());
                return List.of();
            }

            @Override
            public void claimTimeSlot(Long doctorId, Long patientId, LocalDateTime startTime) {
                String message = extractMessage(cause,
                        "Scheduling service is temporarily unavailable. Please try again.");
                logger.error("FALLBACK: claimTimeSlot failed. doctorId={}, patientId={}, startTime={}. Cause: {}",
                        doctorId, patientId, startTime, cause.getMessage());
                throw new BusinessValidationException(message);
            }

            @Override
            public void cancelBooking(Long doctorId, Long patientId, LocalDateTime startTime) {
                String message = extractMessage(cause,
                        "Scheduling service is temporarily unavailable. Please try again.");
                logger.error("FALLBACK: cancelBooking failed. doctorId={}, patientId={}, startTime={}. Cause: {}",
                        doctorId, patientId, startTime, cause.getMessage());
                throw new RuntimeException(message);
            }
        };
    }
}