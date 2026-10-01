package com.hospital.appointmentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.appointmentservice.dto.*;
import com.hospital.appointmentservice.exception.GlobalExceptionHandler;
import com.hospital.appointmentservice.exception.OwnershipException;
import com.hospital.appointmentservice.model.Appointment;
import com.hospital.appointmentservice.model.AppointmentStatus;
import com.hospital.appointmentservice.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // CHANGED: was org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@DisplayName("AppointmentController Tests")
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppointmentService appointmentService;

    private GetAvailableDoctorsRequest getAvailableDoctorsRequest;
    private GetTimeSlotsRequest getTimeSlotsRequest;
    private BookAppointmentRequest bookAppointmentRequest;
    private RescheduleAppointmentRequest rescheduleAppointmentRequest;
    private Appointment mockAppointment;

    @BeforeEach
    void setUp() {
        getAvailableDoctorsRequest = new GetAvailableDoctorsRequest(101L, "Cardiology");
        getTimeSlotsRequest = new GetTimeSlotsRequest(101L, 201L, LocalDate.of(2030, 12, 15));
        bookAppointmentRequest = new BookAppointmentRequest(101L, 201L, LocalDateTime.of(2030, 12, 15, 10, 0));
        rescheduleAppointmentRequest = new RescheduleAppointmentRequest(
                "550e8400-e29b-41d4-a716-446655440000",
                LocalDateTime.of(2030, 12, 20, 14, 0));

        mockAppointment = new Appointment();
        mockAppointment.setAppointmentId(1000L);
        mockAppointment.setPatientId(101L);
        mockAppointment.setDoctorId(201L);
        mockAppointment.setConfirmationCode("550e8400-e29b-41d4-a716-446655440000");
        mockAppointment.setAppointmentDate(LocalDateTime.of(2030, 12, 15, 10, 0));
        mockAppointment.setStatus(AppointmentStatus.CONFIRMED);
    }

    // ==================== GET AVAILABLE DOCTORS ====================

    @Test
    @DisplayName("POST /available-doctors - Success with valid request")
    void testGetAvailableDoctors_Success() throws Exception {
        List<DoctorAvailabilityDTO> mockDoctors = Arrays.asList(
                new DoctorAvailabilityDTO(201L, "Dr. Smith", "Cardiology"),
                new DoctorAvailabilityDTO(202L, "Dr. Johnson", "Cardiology")
        );
        when(appointmentService.getAvailableDoctorsBySpecialization(any(GetAvailableDoctorsRequest.class)))
                .thenReturn(mockDoctors);

        mockMvc.perform(post("/api/v1/appointments/available-doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(getAvailableDoctorsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].doctorId").value(201L));
    }

    @Test
    @DisplayName("POST /available-doctors - Returns 400 when specialization is blank")
    void testGetAvailableDoctors_InvalidRequest() throws Exception {
        GetAvailableDoctorsRequest invalidRequest = new GetAvailableDoctorsRequest(101L, "");

        mockMvc.perform(post("/api/v1/appointments/available-doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET TIME SLOTS ====================

    @Test
    @DisplayName("POST /available-slots - Success with valid request")
    void testGetAvailableSlots_Success() throws Exception {
        List<TimeSlotDTO> mockSlots = Arrays.asList(
                new TimeSlotDTO(LocalDateTime.of(2030, 12, 15, 9, 0), false),
                new TimeSlotDTO(LocalDateTime.of(2030, 12, 15, 11, 0), true)
        );
        when(appointmentService.getTimeSlotsForDoctor(any(GetTimeSlotsRequest.class)))
                .thenReturn(mockSlots);

        mockMvc.perform(post("/api/v1/appointments/available-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(getTimeSlotsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].booked").value(true));
    }

    @Test
    @DisplayName("POST /available-slots - Returns 400 when doctorId is null")
    void testGetAvailableSlots_InvalidRequest() throws Exception {
        GetTimeSlotsRequest invalidRequest = new GetTimeSlotsRequest(101L, null, LocalDate.of(2030, 4, 15));

        mockMvc.perform(post("/api/v1/appointments/available-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== BOOK APPOINTMENT ====================

    @Test
    @DisplayName("POST /booking - Returns 201 Created on success")
    void testBookAppointment_Success() throws Exception {
        when(appointmentService.bookAppointment(any(BookAppointmentRequest.class)))
                .thenReturn(mockAppointment);

        mockMvc.perform(post("/api/v1/appointments/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookAppointmentRequest)))
                .andExpect(status().isCreated()) // CHANGED: was isOk() — booking creates a resource, correct HTTP status is 201
                .andExpect(jsonPath("$.appointmentId").value(1000L))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /booking - Returns 400 when startTime is null")
    void testBookAppointment_InvalidRequest() throws Exception {
        BookAppointmentRequest invalidRequest = new BookAppointmentRequest(101L, 201L, null);

        mockMvc.perform(post("/api/v1/appointments/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================== CANCEL APPOINTMENT ====================

    @Test
    @DisplayName("PATCH /cancel/{confirmationCode} - Success with valid confirmation code")
    void testCancelAppointment_Success() throws Exception {
        Appointment cancelledAppointment = new Appointment();
        cancelledAppointment.setConfirmationCode("550e8400-e29b-41d4-a716-446655440000");
        cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentService.cancelAppointment(anyString(), any(), any()))
                .thenReturn(cancelledAppointment);

        // CHANGED: was put() — cancel is a partial status update, correct HTTP method is PATCH
        mockMvc.perform(patch("/api/v1/appointments/cancel/550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ==================== CANCEL — ownership returns 403 ====================

    @Test
    @DisplayName("PATCH /cancel/{code} - Cross-patient cancel returns 403, not 400")
    void testCancelAppointment_differentPatient_returns403() throws Exception {
        when(appointmentService.cancelAppointment(anyString(), any(), any()))
                .thenThrow(new OwnershipException("Access denied: you can only manage your own appointments."));

        mockMvc.perform(patch("/api/v1/appointments/cancel/550e8400-e29b-41d4-a716-446655440000")
                        .header("X-Service-Id", "2")
                        .header("X-User-Role", "ROLE_PATIENT"))
                .andExpect(status().isForbidden());
    }

    // ==================== COMPLETE APPOINTMENT ====================

    @Test
    @DisplayName("PATCH /complete/{confirmationCode} - Success returns completed appointment")
    void testCompleteAppointment_Success() throws Exception {
        Appointment completedAppointment = new Appointment();
        completedAppointment.setConfirmationCode("550e8400-e29b-41d4-a716-446655440000");
        completedAppointment.setStatus(AppointmentStatus.COMPLETED);

        when(appointmentService.completeAppointment(anyString(), any(), any()))
                .thenReturn(completedAppointment);

        mockMvc.perform(patch("/api/v1/appointments/complete/550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // ==================== RESCHEDULE APPOINTMENT ====================

    @Test
    @DisplayName("PATCH /reschedule - Success with valid request")
    void testRescheduleAppointment_Success() throws Exception {
        when(appointmentService.rescheduleAppointment(any(RescheduleAppointmentRequest.class), any(), any()))
                .thenReturn(mockAppointment);

        mockMvc.perform(patch("/api/v1/appointments/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rescheduleAppointmentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmationCode").value("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    @DisplayName("PATCH /reschedule - Returns 400 when newAppointmentTime is null")
    void testRescheduleAppointment_InvalidRequest() throws Exception {
        RescheduleAppointmentRequest invalidRequest = new RescheduleAppointmentRequest(
                "550e8400-e29b-41d4-a716-446655440000", null);

        mockMvc.perform(patch("/api/v1/appointments/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}