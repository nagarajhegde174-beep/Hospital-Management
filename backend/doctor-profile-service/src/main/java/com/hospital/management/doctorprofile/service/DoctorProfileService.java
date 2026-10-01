package com.hospital.management.doctorprofile.service;

import com.hospital.management.doctorprofile.dto.DoctorAppointmentResponseDTO;
import com.hospital.management.doctorprofile.dto.DoctorProfileResponseDTO;
import com.hospital.management.doctorprofile.dto.DoctorRegistrationDTO;
import com.hospital.management.doctorprofile.dto.DoctorUpdateDTO;

import java.util.List;

public interface DoctorProfileService {
    DoctorProfileResponseDTO registerDoctor(DoctorRegistrationDTO requestDTO);
    DoctorProfileResponseDTO getDoctorById(Long id);
    List<DoctorProfileResponseDTO> getAllDoctors();
    List<DoctorAppointmentResponseDTO> getDoctorsBySpecialization(String specialization);
    DoctorProfileResponseDTO updateDoctor(Long id, DoctorUpdateDTO updateDTO);
    String deleteDoctorById(Long id);
}