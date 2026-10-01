package com.hospital.management.patientservice.service;

import com.hospital.management.patientservice.client.AuthServiceClient;
import com.hospital.management.patientservice.dto.PatientRegistrationDTO;
import com.hospital.management.patientservice.dto.PatientResponseDTO;
import com.hospital.management.patientservice.dto.PatientUpdateDTO;
import com.hospital.management.patientservice.exception.PatientNotFoundException;
import com.hospital.management.patientservice.model.Patient;
import com.hospital.management.patientservice.repository.PatientRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.print.attribute.standard.PagesPerMinute;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final ModelMapper modelMapper;
    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    public PatientResponseDTO registerPatient(PatientRegistrationDTO dto) {
        log.info("Registering patient: {}", dto.getName());
        Patient patient = modelMapper.map(dto, Patient.class);
        Patient saved = patientRepository.save(patient);
        return modelMapper.map(saved, PatientResponseDTO.class);
    }

    @Override
    public PatientResponseDTO getPatientById(long patientId) {
        log.debug("Fetching patient ID: {}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> {
                    log.error("Patient not found: ID {}", patientId);
                    return new PatientNotFoundException(
                            "Patient not found with ID: " + patientId);
                });

        return modelMapper.map(patient, PatientResponseDTO.class);
    }

    @Override
    @Transactional
    public PatientResponseDTO updatePatientById(long patientId, PatientUpdateDTO dto) {
        log.info("Updating patient ID: {}", patientId);

        Patient existing = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(
                        "Patient not found with ID: " + patientId));

        // Only update fields that are provided — partial update support
        if (dto.getName() != null) {
            existing.setName(dto.getName());
        }
        if (dto.getDateOfBirth() != null) {
            existing.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getContactDetails() != null) {
            existing.setContactDetails(dto.getContactDetails());
        }
        if (dto.getGender() != null) {
            existing.setGender(dto.getGender());
        }

        Patient updated = patientRepository.save(existing);

        log.info("Patient updated successfully: {}", updated.getName());
        return modelMapper.map(updated, PatientResponseDTO.class);
    }

    @Override
    public List<PatientResponseDTO> getAllPatient() {
        return patientRepository.findAll()
                .stream()
                .map(patient -> modelMapper.map(patient, PatientResponseDTO.class))
                .toList();
    }

    @Override
    public String deletePatientById(long patientId) {
        log.debug("Deleting patient ID: {}", patientId);

        patientRepository.findById(patientId)
                .orElseThrow(() -> {
                    log.error("Patient not found: ID {}", patientId);
                    return new PatientNotFoundException(
                            "Patient not found with ID: " + patientId);
                });
        patientRepository.deleteById(patientId);
        String email = authServiceClient.getPatientEmail(patientId);
        authServiceClient.deleteUserByEmail(email);
        return "Successfully Deleted Id " + patientId;
    }
}