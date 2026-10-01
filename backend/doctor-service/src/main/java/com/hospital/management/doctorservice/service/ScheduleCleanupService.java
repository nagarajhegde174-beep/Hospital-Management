package com.hospital.management.doctorservice.service;

import com.hospital.management.doctorservice.repository.DoctorAvailabilityRepository;
import com.hospital.management.doctorservice.repository.DoctorSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleCleanupService {

    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorSlotRepository slotRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void wipeOldSchedules() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Midnight cleanup started. Removing records before {}", yesterday);

        availabilityRepository.deleteByDateBefore(yesterday);
        slotRepository.deleteBySlotDateBefore(yesterday);

        log.info("Midnight cleanup completed successfully for date boundary: {}", yesterday);
    }
}