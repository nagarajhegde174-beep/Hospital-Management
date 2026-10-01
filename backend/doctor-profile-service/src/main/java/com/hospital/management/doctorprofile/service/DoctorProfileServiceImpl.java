package com.hospital.management.doctorprofile.service;

import com.hospital.management.doctorprofile.client.AuthServiceClient;
import com.hospital.management.doctorprofile.dto.DoctorAppointmentResponseDTO;
import com.hospital.management.doctorprofile.dto.DoctorRegistrationDTO;
import com.hospital.management.doctorprofile.dto.DoctorProfileResponseDTO;
import com.hospital.management.doctorprofile.dto.DoctorUpdateDTO;
import com.hospital.management.doctorprofile.entity.Doctor;
import com.hospital.management.doctorprofile.exception.DoctorAlreadyExistsException;
import com.hospital.management.doctorprofile.exception.DoctorNotFoundException;
import com.hospital.management.doctorprofile.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorProfileServiceImpl implements DoctorProfileService {

    private final DoctorRepository doctorRepository;
    private final ModelMapper modelMapper;
    private final AuthServiceClient authServiceClient;

    @Transactional
    public DoctorProfileResponseDTO registerDoctor(DoctorRegistrationDTO requestDTO) {
        log.info("Registering new doctor with email={}", requestDTO.getEmail());

        if (doctorRepository.existsByEmail(requestDTO.getEmail())) {
            throw new DoctorAlreadyExistsException(
                    "Doctor with email " + requestDTO.getEmail() + " already exists.");
        }

        Doctor doctor = modelMapper.map(requestDTO, Doctor.class);
        Doctor saved = doctorRepository.save(doctor);
        log.info("Doctor registered successfully with id={}", saved.getId());

        return modelMapper.map(saved, DoctorProfileResponseDTO.class);
    }

    public DoctorProfileResponseDTO getDoctorById(Long id) {
        log.info("Fetching doctor with id={}", id);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new DoctorNotFoundException(
                        "Doctor not found with id: " + id, HttpStatus.NOT_FOUND));

        return modelMapper.map(doctor, DoctorProfileResponseDTO.class);
    }

    public List<DoctorProfileResponseDTO> getAllDoctors() {
        log.info("Fetching all doctors");

        return doctorRepository.findAll()
                .stream()
                .map(doctor -> modelMapper.map(doctor, DoctorProfileResponseDTO.class))
                .toList();
    }

    public List<DoctorAppointmentResponseDTO> getDoctorsBySpecialization(
            String specialization) {
        log.info("Searching doctors by specialization={}", specialization);

        List<Doctor> doctors = doctorRepository
                .findBySpecializationIgnoreCase(specialization);

        if (doctors.isEmpty()) {
            return List.of();
        }

        return doctors.stream()
                .map(doctor -> DoctorAppointmentResponseDTO.builder()
                        .doctorId(doctor.getId())
                        .doctorName(doctor.getName())
                        .specialization(doctor.getSpecialization())
                        .designation(doctor.getDesignation())
                        .build())
                .toList();

    }

    @Transactional
    public DoctorProfileResponseDTO updateDoctor(Long id, DoctorUpdateDTO updateDTO) {
        log.info("Updating doctor with id={}", id);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new DoctorNotFoundException(
                        "Doctor not found with id: " + id, HttpStatus.NOT_FOUND));

        if (updateDTO.getName() != null) {
            doctor.setName(updateDTO.getName());
        }
        if (updateDTO.getContactDetails() != null) {
            doctor.setContactDetails(updateDTO.getContactDetails());
        }
        if (updateDTO.getSpecialization() != null) {
            doctor.setSpecialization(updateDTO.getSpecialization());
        }
        if (updateDTO.getDesignation() != null) {
            doctor.setDesignation(updateDTO.getDesignation());
        }

        Doctor updated = doctorRepository.save(doctor);
        log.info("Doctor id={} updated successfully", id);

        return modelMapper.map(updated, DoctorProfileResponseDTO.class);
    }

    @Override
    public String deleteDoctorById(Long id) {
        doctorRepository.findById(id)
                .orElseThrow(() -> new DoctorNotFoundException(
                        "Doctor not found with id: " + id, HttpStatus.NOT_FOUND));

        String email = authServiceClient.getDoctorEmail(id);
        authServiceClient.deleteUserByEmail(email);

        doctorRepository.deleteById(id);
        return "Successfully Deleted Id " + id;
    }
}