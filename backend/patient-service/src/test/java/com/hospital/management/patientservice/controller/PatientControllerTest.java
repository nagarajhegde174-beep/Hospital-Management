package com.hospital.management.patientservice.controller;

import com.hospital.management.patientservice.exception.GlobalExceptionHandler;
import com.hospital.management.patientservice.exception.PatientNotFoundException;
import com.hospital.management.patientservice.dto.PatientResponseDTO;
import com.hospital.management.patientservice.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("PatientController Tests")
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @Test
    @DisplayName("GET /patients/{id} - ROLE_PATIENT reading own record returns 200")
    void getPatient_ownRecord_returns200() throws Exception {
        PatientResponseDTO dto = new PatientResponseDTO();
        dto.setPatientId(1L);
        dto.setName("Aarav Sharma");
        dto.setDateOfBirth(LocalDate.of(1990, 5, 15));
        dto.setGender("MALE");
        dto.setContactDetails("9876543210");

        when(patientService.getPatientById(eq(1L))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/patients/1")
                        .header("X-Service-Id", "1")
                        .header("X-User-Role", "ROLE_PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aarav Sharma"));
    }

    @Test
    @DisplayName("GET /patients/{id} - ROLE_PATIENT reading another patient's record returns 403 not 500")
    void getPatient_byOtherPatient_returns403_not500() throws Exception {
        mockMvc.perform(get("/api/v1/patients/2")
                        .header("X-Service-Id", "1")
                        .header("X-User-Role", "ROLE_PATIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /patients/{id} - ROLE_DOCTOR reading any patient returns 200")
    void getPatient_byDoctor_returns200() throws Exception {
        PatientResponseDTO dto = new PatientResponseDTO();
        dto.setPatientId(2L);
        dto.setName("Priya Kapoor");

        when(patientService.getPatientById(eq(2L))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/patients/2")
                        .header("X-Service-Id", "1")
                        .header("X-User-Role", "ROLE_DOCTOR"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /patients/{id} - Patient not found returns 404")
    void getPatient_notFound_returns404() throws Exception {
        when(patientService.getPatientById(eq(99L)))
                .thenThrow(new PatientNotFoundException("Patient not found with id: 99"));

        mockMvc.perform(get("/api/v1/patients/99")
                        .header("X-Service-Id", "99")
                        .header("X-User-Role", "ROLE_PATIENT"))
                .andExpect(status().isNotFound());
    }
}
