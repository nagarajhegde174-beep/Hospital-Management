package com.hospital.management.doctorservice.controller;

import com.hospital.management.doctorservice.exception.GlobalExceptionHandler;
import com.hospital.management.doctorservice.service.DoctorScheduleInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorScheduleController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("DoctorScheduleController Tests")
class DoctorScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorScheduleInterface scheduleService;

    private String availabilityJson(String date) {
        return """
                {
                  "doctorId": 1,
                  "date": "%s",
                  "shiftStart": "09:00:00",
                  "shiftEnd": "17:00:00",
                  "breakStart": "13:00:00"
                }
                """.formatted(date);
    }

    @Test
    @DisplayName("POST /set-availability - Concurrent duplicate returns 409, not 500")
    void setAvailability_concurrentDuplicate_returns409() throws Exception {
        when(scheduleService.createMonthlySchedule(any()))
                .thenThrow(new DataIntegrityViolationException("uq_availability_doctor_date"));

        mockMvc.perform(post("/api/v1/doctor-schedule/set-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availabilityJson(LocalDate.now().plusDays(5).toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This resource already exists or violates a uniqueness constraint."));
    }

    @Test
    @DisplayName("POST /set-availability - Optimistic lock conflict returns 409, not 500")
    void setAvailability_optimisticLockConflict_returns409() throws Exception {
        when(scheduleService.createMonthlySchedule(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Object.class, 1L));

        mockMvc.perform(post("/api/v1/doctor-schedule/set-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availabilityJson(LocalDate.now().plusDays(5).toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This slot was just booked by another patient. Please retry."));
    }
}
