package com.hospital.management.doctorservice.service;

import com.hospital.management.doctorservice.client.DoctorClient;
import com.hospital.management.doctorservice.client.MedicalHistoryClient;
import com.hospital.management.doctorservice.dto.DoctorAvailabilityRequestDTO;
import com.hospital.management.doctorservice.dto.DoctorResponseDTO;
import com.hospital.management.doctorservice.entity.DoctorAvailability;
import com.hospital.management.doctorservice.exception.ScheduleException;
import com.hospital.management.doctorservice.repository.DoctorAvailabilityRepository;
import com.hospital.management.doctorservice.repository.DoctorSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorScheduleServiceTest {

    @Mock
    private DoctorAvailabilityRepository availabilityRepository;

    @Mock
    private DoctorSlotRepository slotRepository;

    @Mock
    private MedicalHistoryClient historyClient;

    @Mock
    private DoctorClient doctorClient;


    private ModelMapper modelMapper = new ModelMapper();

    private DoctorScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new DoctorScheduleService(
                availabilityRepository,
                slotRepository,
                historyClient,

                doctorClient
        );
    }

    @Test
    void createMonthlySchedule_shouldThrowException_whenDateIsLessThan3DaysInAdvance() {

        DoctorAvailabilityRequestDTO requestDTO = new DoctorAvailabilityRequestDTO();
        requestDTO.setDoctorId(1L);
        requestDTO.setDate(LocalDate.now().plusDays(1)); // only 1 day ahead — must fail
        requestDTO.setShiftStart(LocalTime.of(9, 0));
        requestDTO.setShiftEnd(LocalTime.of(17, 0));

        DoctorResponseDTO fakeDoctor = new DoctorResponseDTO();
        fakeDoctor.setId(1L);
        fakeDoctor.setName("Dr. Arjun");
        when(doctorClient.getDoctorById(1L)).thenReturn(fakeDoctor);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.createMonthlySchedule(requestDTO)
        );

        assertTrue(exception.getMessage().contains("at least 3 days in advance"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        verify(availabilityRepository, never()).save(any());
    }

    // Test 2
    @Test
    void createMonthlySchedule_shouldSucceed_whenDateIsValidAndShiftIsCorrect() {

        DoctorAvailabilityRequestDTO requestDTO = new DoctorAvailabilityRequestDTO();
        requestDTO.setDoctorId(1L);
        requestDTO.setDate(LocalDate.now().plusDays(5)); // 5 days ahead — passes 3-day rule
        requestDTO.setShiftStart(LocalTime.of(9, 0));
        requestDTO.setShiftEnd(LocalTime.of(18, 0));     // 9-hour shift, no break

        DoctorResponseDTO fakeDoctor = new DoctorResponseDTO();
        fakeDoctor.setId(1L);
        fakeDoctor.setName("Dr. Arjun");
        when(doctorClient.getDoctorById(1L)).thenReturn(fakeDoctor);

        when(availabilityRepository.findByDoctorIdAndDate(1L, LocalDate.now().plusDays(5)))
                .thenReturn(Optional.empty());

        when(slotRepository.saveAll(any())).thenReturn(List.of());

        String result = scheduleService.createMonthlySchedule(requestDTO);

        assertTrue(result.contains("Schedule created and locked successfully"));

        verify(availabilityRepository, times(1)).save(any());

        verify(slotRepository, times(1)).saveAll(any());
    }

//   Test 3
    @Test
    void createMonthlySchedule_shouldThrowException_whenShiftEndIsBeforeShiftStart() {

        DoctorAvailabilityRequestDTO requestDTO = new DoctorAvailabilityRequestDTO();
        requestDTO.setDoctorId(1L);
        requestDTO.setDate(LocalDate.now().plusDays(5)); // valid date — passes validations 0 and 3
        requestDTO.setShiftStart(LocalTime.of(17, 0));   // 5 PM start
        requestDTO.setShiftEnd(LocalTime.of(9, 0));      // 9 AM end — before start, must fail

        DoctorResponseDTO fakeDoctor = new DoctorResponseDTO();
        fakeDoctor.setId(1L);
        fakeDoctor.setName("Dr. Arjun");
        when(doctorClient.getDoctorById(1L)).thenReturn(fakeDoctor);

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.createMonthlySchedule(requestDTO)
        );

        assertTrue(exception.getMessage().contains("Shift end time must be after shift start time"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        verify(availabilityRepository, never()).save(any());
    }

    // Test 4

    @Test
    void createMonthlySchedule_shouldThrowException_whenScheduleAlreadyExists() {

        LocalDate futureDate = LocalDate.now().plusDays(5);

        DoctorAvailabilityRequestDTO requestDTO = new DoctorAvailabilityRequestDTO();
        requestDTO.setDoctorId(1L);
        requestDTO.setDate(futureDate);
        requestDTO.setShiftStart(LocalTime.of(9, 0));
        requestDTO.setShiftEnd(LocalTime.of(17, 0));

        DoctorResponseDTO fakeDoctor = new DoctorResponseDTO();
        fakeDoctor.setId(1L);
        fakeDoctor.setName("Dr. Arjun");
        when(doctorClient.getDoctorById(1L)).thenReturn(fakeDoctor);

        DoctorAvailability existingAvailability = new DoctorAvailability();
        existingAvailability.setDoctorId(1L);
        existingAvailability.setDate(futureDate);
        existingAvailability.setLocked(true);
        when(availabilityRepository.findByDoctorIdAndDate(1L, futureDate))
                .thenReturn(Optional.of(existingAvailability));

        ScheduleException exception = assertThrows(
                ScheduleException.class,
                () -> scheduleService.createMonthlySchedule(requestDTO)
        );

        assertTrue(exception.getMessage().contains("already locked"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        verify(availabilityRepository, never()).save(any());
    }

    @Test
    void createMonthlySchedule_concurrentDuplicate_propagatesDataIntegrityViolationException() {
        LocalDate futureDate = LocalDate.now().plusDays(5);

        DoctorAvailabilityRequestDTO requestDTO = new DoctorAvailabilityRequestDTO();
        requestDTO.setDoctorId(1L);
        requestDTO.setDate(futureDate);
        requestDTO.setShiftStart(LocalTime.of(9, 0));
        requestDTO.setShiftEnd(LocalTime.of(17, 0));

        DoctorResponseDTO fakeDoctor = new DoctorResponseDTO();
        fakeDoctor.setId(1L);
        fakeDoctor.setName("Dr. Arjun");
        when(doctorClient.getDoctorById(1L)).thenReturn(fakeDoctor);
        when(availabilityRepository.findByDoctorIdAndDate(1L, futureDate)).thenReturn(Optional.empty());
        when(availabilityRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("uq_availability_doctor_date"));

        assertThrows(DataIntegrityViolationException.class,
                () -> scheduleService.createMonthlySchedule(requestDTO));
    }
}