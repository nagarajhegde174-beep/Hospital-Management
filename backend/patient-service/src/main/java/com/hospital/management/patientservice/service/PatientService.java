package com.hospital.management.patientservice.service;


import com.hospital.management.patientservice.dto.PatientRegistrationDTO;
import com.hospital.management.patientservice.dto.PatientResponseDTO;
import com.hospital.management.patientservice.dto.PatientUpdateDTO;

import java.util.List;

public interface PatientService {

    PatientResponseDTO registerPatient(PatientRegistrationDTO dto);

    PatientResponseDTO getPatientById(long patientId);

    PatientResponseDTO updatePatientById(long patientId, PatientUpdateDTO dto);

    List<PatientResponseDTO> getAllPatient();

    String deletePatientById(long patientId);
}