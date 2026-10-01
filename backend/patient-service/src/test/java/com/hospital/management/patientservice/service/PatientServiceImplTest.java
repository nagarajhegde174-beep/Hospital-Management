package com.hospital.management.patientservice.service;

import com.hospital.management.patientservice.client.AuthServiceClient;
import com.hospital.management.patientservice.dto.PatientRegistrationDTO;
import com.hospital.management.patientservice.dto.PatientResponseDTO;
import com.hospital.management.patientservice.dto.PatientUpdateDTO;
import com.hospital.management.patientservice.exception.PatientNotFoundException;
import com.hospital.management.patientservice.model.Patient;
import com.hospital.management.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock private PatientRepository patientRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private AuthServiceClient authServiceClient;

    @InjectMocks
    private PatientServiceImpl patientService;

    // ═══════════════════════════════════════════════════════════
    // registerPatient
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("registerPatient: saves patient and returns response DTO")
    void registerPatient_success() {
        PatientRegistrationDTO dto = new PatientRegistrationDTO();
        dto.setName("Santhoshkumar");

        Patient patient = patient(1L, "Santhoshkumar");
        PatientResponseDTO responseDTO = responseDTO(1L, "Santhoshkumar");

        when(modelMapper.map(dto, Patient.class)).thenReturn(patient);
        when(patientRepository.save(patient)).thenReturn(patient);
        when(modelMapper.map(patient, PatientResponseDTO.class)).thenReturn(responseDTO);

        PatientResponseDTO result = patientService.registerPatient(dto);

        assertNotNull(result);
        assertEquals(1L, result.getPatientId());
        assertEquals("Santhoshkumar", result.getName());
        verify(patientRepository).save(patient);
    }

    // ═══════════════════════════════════════════════════════════
    // getPatientById
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("getPatientById: returns patient when found")
    void getPatientById_success() {
        Patient patient = patient(1L, "Kumar");
        PatientResponseDTO responseDTO = responseDTO(1L, "Kumar");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(modelMapper.map(patient, PatientResponseDTO.class)).thenReturn(responseDTO);

        PatientResponseDTO result = patientService.getPatientById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPatientId());
        verify(patientRepository).findById(1L);
    }

    @Test
    @DisplayName("getPatientById: throws PatientNotFoundException when not found")
    void getPatientById_notFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class,
                () -> patientService.getPatientById(99L));
        verify(patientRepository).findById(99L);
    }

    // ═══════════════════════════════════════════════════════════
    // updatePatientById
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("updatePatientById: updates patient fields and returns updated DTO")
    void updatePatientById_success() {
        Patient existing = patient(1L, "Kumar");

        PatientUpdateDTO updateDTO = new PatientUpdateDTO();
        updateDTO.setName("Kumar Updated");
        updateDTO.setContactDetails("9999999999");

        Patient updated = patient(1L, "Kumar Updated");
        PatientResponseDTO responseDTO = responseDTO(1L, "Kumar Updated");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.save(existing)).thenReturn(updated);
        when(modelMapper.map(updated, PatientResponseDTO.class)).thenReturn(responseDTO);

        PatientResponseDTO result = patientService.updatePatientById(1L, updateDTO);

        assertNotNull(result);
        assertEquals("Kumar Updated", result.getName());
        verify(patientRepository).findById(1L);
        verify(patientRepository).save(existing);
    }

    @Test
    @DisplayName("updatePatientById: throws PatientNotFoundException when patient not found")
    void updatePatientById_notFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        PatientUpdateDTO updateDTO = new PatientUpdateDTO();
        updateDTO.setName("Ghost");

        assertThrows(PatientNotFoundException.class,
                () -> patientService.updatePatientById(99L, updateDTO));
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePatientById: partial update — null fields are not overwritten")
    void updatePatientById_partialUpdate_nullFieldsIgnored() {
        Patient existing = patient(1L, "Kumar");
        existing.setContactDetails("1234567890");
        existing.setGender("MALE");

        PatientUpdateDTO updateDTO = new PatientUpdateDTO();
        updateDTO.setName("Kumar Updated");
        // contactDetails and gender left null — should not be overwritten

        Patient saved = patient(1L, "Kumar Updated");
        PatientResponseDTO responseDTO = responseDTO(1L, "Kumar Updated");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientRepository.save(existing)).thenReturn(saved);
        when(modelMapper.map(saved, PatientResponseDTO.class)).thenReturn(responseDTO);

        patientService.updatePatientById(1L, updateDTO);

        assertEquals("1234567890", existing.getContactDetails());
        assertEquals("MALE", existing.getGender());
    }

    // ═══════════════════════════════════════════════════════════
    // getAllPatient
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("getAllPatient: returns list of all patients")
    void getAllPatient_returnsList() {
        Patient p1 = patient(1L, "Kumar");
        Patient p2 = patient(2L, "Ravi");
        PatientResponseDTO r1 = responseDTO(1L, "Kumar");
        PatientResponseDTO r2 = responseDTO(2L, "Ravi");

        when(patientRepository.findAll()).thenReturn(List.of(p1, p2));
        when(modelMapper.map(p1, PatientResponseDTO.class)).thenReturn(r1);
        when(modelMapper.map(p2, PatientResponseDTO.class)).thenReturn(r2);

        List<PatientResponseDTO> result = patientService.getAllPatient();

        assertEquals(2, result.size());
        verify(patientRepository).findAll();
    }

    @Test
    @DisplayName("getAllPatient: returns empty list when no patients exist")
    void getAllPatient_emptyList() {
        when(patientRepository.findAll()).thenReturn(List.of());

        List<PatientResponseDTO> result = patientService.getAllPatient();

        assertTrue(result.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════
    // deletePatientById
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("deletePatientById: deletes patient and removes auth user")
    void deletePatientById_success() {
        Patient patient = patient(1L, "Kumar");

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        doNothing().when(patientRepository).deleteById(1L);
        when(authServiceClient.getPatientEmail(1L)).thenReturn("kumar@test.com");
        doNothing().when(authServiceClient).deleteUserByEmail("kumar@test.com");

        String result = patientService.deletePatientById(1L);

        assertEquals("Successfully Deleted Id 1", result);
        verify(patientRepository).deleteById(1L);
        verify(authServiceClient).getPatientEmail(1L);
        verify(authServiceClient).deleteUserByEmail("kumar@test.com");
    }

    @Test
    @DisplayName("deletePatientById: throws PatientNotFoundException when patient not found")
    void deletePatientById_notFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class,
                () -> patientService.deletePatientById(99L));
        verify(patientRepository, never()).deleteById(anyLong());
        verifyNoInteractions(authServiceClient);
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    private Patient patient(Long id, String name) {
        Patient p = new Patient();
        p.setPatientId(id);
        p.setName(name);
        return p;
    }

    private PatientResponseDTO responseDTO(Long id, String name) {
        PatientResponseDTO dto = new PatientResponseDTO();
        dto.setPatientId(id);
        dto.setName(name);
        return dto;
    }
}