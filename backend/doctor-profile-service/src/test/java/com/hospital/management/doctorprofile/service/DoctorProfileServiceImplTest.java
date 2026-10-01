package com.hospital.management.doctorprofile.service;

import com.hospital.management.doctorprofile.client.AuthServiceClient;
import com.hospital.management.doctorprofile.dto.DoctorAppointmentResponseDTO;
import com.hospital.management.doctorprofile.dto.DoctorProfileResponseDTO;
import com.hospital.management.doctorprofile.dto.DoctorRegistrationDTO;
import com.hospital.management.doctorprofile.dto.DoctorUpdateDTO;
import com.hospital.management.doctorprofile.entity.Doctor;
import com.hospital.management.doctorprofile.exception.DoctorAlreadyExistsException;
import com.hospital.management.doctorprofile.exception.DoctorNotFoundException;
import com.hospital.management.doctorprofile.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorProfileServiceImplTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private DoctorProfileServiceImpl doctorProfileService;

    private Doctor doctor;
    private DoctorRegistrationDTO registrationDTO;
    private DoctorUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        doctor = Doctor.builder()
                .id(1L)
                .name("Dr. Arun")
                .email("arun@hospital.com")
                .contactDetails("9876543210")
                .specialization("Cardiologist")
                .designation("Consultant")
                .build();

        registrationDTO = new DoctorRegistrationDTO();
        registrationDTO.setName("Dr. Arun");
        registrationDTO.setEmail("arun@hospital.com");
        registrationDTO.setContactDetails("9876543210");
        registrationDTO.setSpecialization("Cardiologist");
        registrationDTO.setDesignation("Consultant");

        updateDTO = new DoctorUpdateDTO();
        updateDTO.setName("Dr. Arun Updated");
        updateDTO.setContactDetails("9898989892");
    }

    // ═══════════════════════════════════════════════════════════
    // registerDoctor
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("registerDoctor — Should save and return DTO")
    void registerDoctor_Success() {
        when(doctorRepository.existsByEmail("arun@hospital.com")).thenReturn(false);
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        DoctorProfileResponseDTO result = doctorProfileService.registerDoctor(registrationDTO);

        assertNotNull(result);
        assertEquals("Dr. Arun", result.getName());
        assertEquals("arun@hospital.com", result.getEmail());
        verify(doctorRepository, times(1)).save(any(Doctor.class));
    }

    @Test
    @DisplayName("registerDoctor — Should throw DoctorAlreadyExistsException on duplicate email")
    void registerDoctor_DuplicateEmail_ThrowsException() {
        when(doctorRepository.existsByEmail("arun@hospital.com")).thenReturn(true);

        assertThrows(DoctorAlreadyExistsException.class,
                () -> doctorProfileService.registerDoctor(registrationDTO));

        verify(doctorRepository, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════
    // getDoctorById
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("getDoctorById — Should return DTO when found")
    void getDoctorById_Found() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        DoctorProfileResponseDTO result = doctorProfileService.getDoctorById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Dr. Arun", result.getName());
        verify(doctorRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getDoctorById — Should throw DoctorNotFoundException when not found")
    void getDoctorById_NotFound_ThrowsException() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DoctorNotFoundException.class,
                () -> doctorProfileService.getDoctorById(99L));

        verify(doctorRepository, times(1)).findById(99L);
    }

    // ═══════════════════════════════════════════════════════════
    // getAllDoctors
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("getAllDoctors — Should return list of DoctorProfileResponseDTO")
    void getAllDoctors_Success() {
        when(doctorRepository.findAll()).thenReturn(List.of(doctor));

        List<DoctorProfileResponseDTO> result = doctorProfileService.getAllDoctors();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Dr. Arun", result.get(0).getName());
        verify(doctorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllDoctors — Should return empty list when no doctors exist")
    void getAllDoctors_EmptyList() {
        when(doctorRepository.findAll()).thenReturn(List.of());

        List<DoctorProfileResponseDTO> result = doctorProfileService.getAllDoctors();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════
    // getDoctorsBySpecialization
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("getDoctorsBySpecialization — Should return list when found")
    void getDoctorsBySpecialization_Found() {
        when(doctorRepository.findBySpecializationIgnoreCase("Cardiologist"))
                .thenReturn(List.of(doctor));

        List<DoctorAppointmentResponseDTO> result =
                doctorProfileService.getDoctorsBySpecialization("Cardiologist");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Dr. Arun", result.get(0).getDoctorName());
        assertEquals("Cardiologist", result.get(0).getSpecialization());
    }

    @Test
    @DisplayName("getDoctorsBySpecialization — Should return empty list when none found")
    void getDoctorsBySpecialization_NotFound_ReturnsEmptyList() {
        when(doctorRepository.findBySpecializationIgnoreCase("Unknown"))
                .thenReturn(List.of());

        List<DoctorAppointmentResponseDTO> result =
                doctorProfileService.getDoctorsBySpecialization("Unknown");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════
    // updateDoctor
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("updateDoctor — Should update fields and return DTO")
    void updateDoctor_Success() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        DoctorProfileResponseDTO result = doctorProfileService.updateDoctor(1L, updateDTO);

        assertNotNull(result);
        verify(doctorRepository, times(1)).findById(1L);
        verify(doctorRepository, times(1)).save(any(Doctor.class));
    }

    @Test
    @DisplayName("updateDoctor — Should throw DoctorNotFoundException when not found")
    void updateDoctor_NotFound_ThrowsException() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DoctorNotFoundException.class,
                () -> doctorProfileService.updateDoctor(99L, updateDTO));

        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateDoctor — Null fields in updateDTO are not overwritten")
    void updateDoctor_NullFields_NotOverwritten() {
        doctor.setSpecialization("Cardiologist");
        doctor.setDesignation("Consultant");

        DoctorUpdateDTO partialUpdate = new DoctorUpdateDTO();
        partialUpdate.setName("Dr. Arun Updated");
        // specialization and designation left null

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        doctorProfileService.updateDoctor(1L, partialUpdate);

        assertEquals("Cardiologist", doctor.getSpecialization());
        assertEquals("Consultant", doctor.getDesignation());
    }

    // ═══════════════════════════════════════════════════════════
    // deleteDoctorById
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("deleteDoctorById — Should delete and remove auth user")
    void deleteDoctorById_Success() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(authServiceClient.getDoctorEmail(1L)).thenReturn("arun@hospital.com");
        doNothing().when(authServiceClient).deleteUserByEmail("arun@hospital.com");
        doNothing().when(doctorRepository).deleteById(1L);

        String result = doctorProfileService.deleteDoctorById(1L);

        assertEquals("Successfully Deleted Id 1", result);
        verify(doctorRepository).deleteById(1L);
        verify(authServiceClient).getDoctorEmail(1L);
        verify(authServiceClient).deleteUserByEmail("arun@hospital.com");
    }

    @Test
    @DisplayName("deleteDoctorById — Should throw DoctorNotFoundException when not found")
    void deleteDoctorById_NotFound_ThrowsException() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DoctorNotFoundException.class,
                () -> doctorProfileService.deleteDoctorById(99L));

        verify(doctorRepository, never()).deleteById(any());
        verifyNoInteractions(authServiceClient);
    }
}