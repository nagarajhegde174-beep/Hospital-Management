package com.hospital.management.doctorservice.service;

import com.hospital.management.doctorservice.dto.AddPrescriptionRequestDTO;
import com.hospital.management.doctorservice.dto.DoctorAvailabilityRequestDTO;
import com.hospital.management.doctorservice.dto.MedicalHistoryResponseDTO;
import com.hospital.management.doctorservice.dto.TimeSlotDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface DoctorScheduleInterface {

    String createMonthlySchedule(DoctorAvailabilityRequestDTO availabilityDTO);

    void cancelSlotBooking(Long doctorId, Long patientId, LocalDateTime startTime);

    List<MedicalHistoryResponseDTO> getPatientHistoryForDoctor(Long slotId);

    void claimTimeSlot(Long doctorId, Long patientId, LocalDateTime startTime);

    List<TimeSlotDTO> getTimeSlotsByDoctorId(Long doctorId, LocalDate date);

    List<LocalDate> getAvailableDatesForDoctor(Long doctorId);

    MedicalHistoryResponseDTO addPrescription(Long slotId, AddPrescriptionRequestDTO requestDTO);
}