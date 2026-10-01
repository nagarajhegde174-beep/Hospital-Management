package com.hospital.appointmentservice.client;

import com.hospital.appointmentservice.client.fallback.DoctorScheduleClientFallbackFactory;
import com.hospital.appointmentservice.dto.TimeSlotDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "doctor-service",
        fallbackFactory = DoctorScheduleClientFallbackFactory.class)
public interface DoctorSheduleClient {


    @GetMapping("/api/v1/doctor-schedule/available-dates/{doctorId}")
    List<LocalDate> doctorAvailableSlots(
            @PathVariable("doctorId") Long doctorId
    );

    @GetMapping("/api/v1/doctor-schedule/slots/doctor/{doctorId}/{date}")
    List<TimeSlotDTO> getTimeSlotsByDoctorId(
            @PathVariable("doctorId") Long doctorId,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );


    @PutMapping("/api/v1/doctor-schedule/slots/claim")
    void claimTimeSlot(
            @RequestParam("doctorId") Long doctorId,
            @RequestParam("patientId") Long patientId,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime
    );

    @PutMapping("/api/v1/doctor-schedule/cancel-booking")
    void cancelBooking(
            @RequestParam("doctorId") Long doctorId,
            @RequestParam("patientId") Long patientId,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime
    );
}