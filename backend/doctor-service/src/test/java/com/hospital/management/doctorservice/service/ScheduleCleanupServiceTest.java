package com.hospital.management.doctorservice.service;

import com.hospital.management.doctorservice.repository.DoctorAvailabilityRepository;
import com.hospital.management.doctorservice.repository.DoctorSlotRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.*;
@Slf4j
@ExtendWith(MockitoExtension.class)
class ScheduleCleanupServiceTest {

    @Mock
    private DoctorAvailabilityRepository availabilityRepository;

    @Mock
    private DoctorSlotRepository slotRepository;

    @InjectMocks
    private ScheduleCleanupService cleanupService;

    @Test
    @DisplayName("Should call delete methods with yesterday's date when wipeOldSchedules is triggered")
    void testWipeOldSchedules() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        cleanupService.wipeOldSchedules();

        verify(availabilityRepository, times(1)).deleteByDateBefore(yesterday);
        verify(slotRepository, times(1)).deleteBySlotDateBefore(yesterday);

        log.info("Cleanup test passed: Repositories triggered with yesterday boundary.");
    }
}