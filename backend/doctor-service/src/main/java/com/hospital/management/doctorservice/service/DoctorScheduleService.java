package com.hospital.management.doctorservice.service;

import com.hospital.management.doctorservice.client.DoctorClient;
import com.hospital.management.doctorservice.client.MedicalHistoryClient;
import com.hospital.management.doctorservice.dto.*;
import com.hospital.management.doctorservice.entity.DoctorAvailability;
import com.hospital.management.doctorservice.entity.DoctorSlot;
import com.hospital.management.doctorservice.exception.ResourceNotFoundException;
import com.hospital.management.doctorservice.exception.ScheduleException;
import com.hospital.management.doctorservice.repository.DoctorAvailabilityRepository;
import com.hospital.management.doctorservice.repository.DoctorSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorScheduleService implements DoctorScheduleInterface {

    private final DoctorAvailabilityRepository availabilityRepository;
    private final DoctorSlotRepository slotRepository;
    private final MedicalHistoryClient historyClient;
    private final DoctorClient doctorClient;

    private static final int SLOT_DURATION_MINUTES  = 30;
    private static final int SLOTS_BEFORE_BREAK     = 3;
    private static final int BREAK_DURATION_MINUTES = 60;
    private static final int MIN_DAYS_IN_ADVANCE    = 3;
    private static final int MAX_HISTORY_RECORDS    = 3;

    @Override
    @Transactional
    public String createMonthlySchedule(DoctorAvailabilityRequestDTO availabilityDTO) {
        log.info("Creating schedule for doctorId={} on date={}",
                availabilityDTO.getDoctorId(), availabilityDTO.getDate());

        try {
            DoctorResponseDTO doctor = doctorClient.getDoctorById(availabilityDTO.getDoctorId());
            log.info("Doctor verified — name={}, specialization={}",
                    doctor.getName(), doctor.getSpecialization());
        } catch (feign.FeignException.NotFound e) {
            throw new ScheduleException(
                    "Doctor with id=" + availabilityDTO.getDoctorId()
                            + " does not exist. Cannot create schedule.",
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Doctor Service unavailable while verifying doctorId={}. Error: {}",
                    availabilityDTO.getDoctorId(), e.getMessage());
            throw new ScheduleException(
                    "Doctor Service is currently unavailable. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        DoctorAvailability availability = new DoctorAvailability();
        availability.setDoctorId(availabilityDTO.getDoctorId());
        availability.setDate(availabilityDTO.getDate());
        availability.setShiftStart(availabilityDTO.getShiftStart());
        availability.setShiftEnd(availabilityDTO.getShiftEnd());
        availability.setBreakStart(availabilityDTO.getBreakStart());

        if (!availability.getShiftEnd().isAfter(availability.getShiftStart())) {
            throw new ScheduleException(
                    "Shift end time must be after shift start time.",
                    HttpStatus.BAD_REQUEST);
        }

        if (availability.getBreakStart() != null) {
            LocalTime breakEnd = availability.getBreakStart()
                    .plusMinutes(BREAK_DURATION_MINUTES);
            if (!availability.getBreakStart().isAfter(availability.getShiftStart())
                    || !availability.getBreakStart().isBefore(availability.getShiftEnd())) {
                throw new ScheduleException(
                        "Break start time must be within the shift window ("
                                + availability.getShiftStart() + " - "
                                + availability.getShiftEnd() + ").",
                        HttpStatus.BAD_REQUEST);
            }

            if (breakEnd.isAfter(availability.getShiftEnd())) {
                throw new ScheduleException(
                        "Break end time (" + breakEnd + ") exceeds shift end time ("
                                + availability.getShiftEnd() + "). "
                                + "Please shorten the shift or adjust the break time.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), availability.getDate());
        if (daysBetween < MIN_DAYS_IN_ADVANCE) {
            throw new ScheduleException(
                    "Availability must be set at least " + MIN_DAYS_IN_ADVANCE
                            + " days in advance. Days provided: " + daysBetween,
                    HttpStatus.BAD_REQUEST);
        }

        availabilityRepository
                .findByDoctorIdAndDate(availability.getDoctorId(), availability.getDate())
                .ifPresent(existing -> {
                    throw new ScheduleException(
                            "Schedule for " +  availability.getDate()
                                    + " is already locked and cannot be updated.",
                            HttpStatus.CONFLICT);
                });

        availability.setLocked(true);
        availability.setAvailable(true);
        availabilityRepository.save(availability);
        log.info("Availability saved and locked for doctorId={}", availability.getDoctorId());

        int slotsCreated = generateTimeSlots(availability);
        log.info("Generated {} slots for doctorId={} on {}",
                slotsCreated, availability.getDoctorId(), availability.getDate());

        return "Schedule created and locked successfully. Slots generated: " + slotsCreated;
    }

    @Override
    @Transactional
    public void cancelSlotBooking(Long doctorId, Long patientId, LocalDateTime startTime) {
        log.info("Cancel booking — doctorId={}, patientId={}, startTime={}",
                doctorId, patientId, startTime);

        LocalDate slotDate = startTime.toLocalDate();
        LocalTime slotTime = startTime.toLocalTime();

        DoctorSlot slot = slotRepository
                .findByDoctorIdAndSlotDateAndStartTime(doctorId, slotDate, slotTime)
                .orElseThrow(() -> new ScheduleException(
                        "No slot found for doctorId=" + doctorId + " at " + startTime,
                        HttpStatus.NOT_FOUND));

        if (!slot.isBooked()) {
            throw new ScheduleException(
                    "Slot at " + startTime + " is not booked. Cannot cancel.",
                    HttpStatus.BAD_REQUEST);
        }

        if (slot.getPatientId() == null || !slot.getPatientId().equals(patientId)) {
            throw new ScheduleException(
                    "You did not book this slot. Cannot cancel.",
                    HttpStatus.FORBIDDEN);
        }

        slot.setBooked(false);
        slot.setPatientId(null);
        slotRepository.save(slot);
        log.info("Slot at {} for doctorId={} released back to available pool", startTime, doctorId);
    }

    @Override
    public List<MedicalHistoryResponseDTO> getPatientHistoryForDoctor(Long slotId) {
        log.info("Fetching patient history for slotId={}", slotId);

        DoctorSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("DoctorSlot", slotId));

        if (!slot.isBooked() || slot.getPatientId() == null) {
            throw new ScheduleException(
                    "No patient assigned to slotId=" + slotId + ". Slot is not yet booked.",
                    HttpStatus.BAD_REQUEST);
        }

        List<MedicalHistoryResponseDTO> fullHistory;
        try {
            fullHistory = historyClient.getPatientHistory(slot.getPatientId());
            log.info("Fetched {} history records for patientId={}",
                    fullHistory.size(), slot.getPatientId());
        } catch (Exception e) {
            log.warn("Medical history service unavailable for patientId={}. Error: {}",
                    slot.getPatientId(), e.getMessage());
            return List.of();
        }

        return fullHistory.stream()
                .sorted((h1, h2) -> h2.getDiagnosedAt().compareTo(h1.getDiagnosedAt()))
                .limit(MAX_HISTORY_RECORDS)
                .toList();
    }

    @Override
    @Transactional
    public void claimTimeSlot(Long doctorId, Long patientId, LocalDateTime startTime) {
        log.info("Claim request — doctorId={}, patientId={}, startTime={}",
                doctorId, patientId, startTime);

        LocalDate slotDate = startTime.toLocalDate();
        LocalTime slotTime = startTime.toLocalTime();

        DoctorSlot slot = slotRepository
                .findByDoctorIdAndSlotDateAndStartTime(doctorId, slotDate, slotTime)
                .orElseThrow(() -> new ScheduleException(
                        "No slot found for doctorId=" + doctorId + " at " + startTime,
                        HttpStatus.NOT_FOUND));

        if (slot.isBooked()) {
            throw new ScheduleException(
                    "Slot at " + startTime + " for doctorId=" + doctorId
                            + " is already booked by patientId=" + slot.getPatientId(),
                    HttpStatus.CONFLICT);
        }

        slot.setBooked(true);
        slot.setPatientId(patientId);
        slotRepository.save(slot);
        log.info("Slot claimed successfully for patientId={} at {}", patientId, startTime);
    }


    @Override
    public List<TimeSlotDTO> getTimeSlotsByDoctorId(Long doctorId, LocalDate date) {
        log.info("Fetching slots for doctorId={} on date={}", doctorId, date);

        return slotRepository.findByDoctorIdAndSlotDate(doctorId, date)
                .stream()
                .map(slot -> new TimeSlotDTO(
                        LocalDateTime.of(slot.getSlotDate(), slot.getStartTime()),
                        slot.isBooked()
                ))
                .toList();
    }

    @Override
    public List<LocalDate> getAvailableDatesForDoctor(Long doctorId) {
        log.info("Fetching available dates for doctorId={}", doctorId);

        return availabilityRepository
                .findByDoctorIdAndDateGreaterThanEqualAndIsAvailableTrue(
                        doctorId, LocalDate.now())
                .stream()
                .map(DoctorAvailability::getDate)
                .sorted()
                .toList();
    }

    @Override
    public MedicalHistoryResponseDTO addPrescription(Long slotId,
                                                     AddPrescriptionRequestDTO requestDTO) {
        log.info("Adding prescription for slotId={}", slotId);

        DoctorSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("DoctorSlot", slotId));

        if (!slot.isBooked() || slot.getPatientId() == null) {
            throw new ScheduleException(
                    "Cannot add prescription — slotId=" + slotId + " is not booked.",
                    HttpStatus.BAD_REQUEST);
        }

        MedicalHistoryRequestDTO historyRequest = new MedicalHistoryRequestDTO();
        historyRequest.setDiagnosedAt(requestDTO.getVisitDate());
        historyRequest.setDiagnosis(requestDTO.getDiagnosis());
        historyRequest.setPrescribedMeds(requestDTO.getPrescribedMeds());
        historyRequest.setPatientId(slot.getPatientId());
        historyRequest.setDoctorId(slot.getDoctorId());

        try {
            MedicalHistoryResponseDTO responseDTO = historyClient.addPrescription(historyRequest);
            log.info("Prescription added successfully for patientId={} slotId={}",
                    slot.getPatientId(), slotId);
            return responseDTO;
        } catch (Exception e) {
            log.error("Failed to add prescription for slotId={}. Error: {}",
                    slotId, e.getMessage());
            throw new ScheduleException(
                    "Failed to save prescription. Please try again.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private int generateTimeSlots(DoctorAvailability availability) {
        List<DoctorSlot> slots = new ArrayList<>();
        LocalTime currentTime = availability.getShiftStart();
        LocalTime shiftEnd    = availability.getShiftEnd();

        LocalTime breakStart = availability.getBreakStart();
        LocalTime breakEnd   = (breakStart != null)
                ? breakStart.plusMinutes(BREAK_DURATION_MINUTES)
                : null;

        int slotCount = 0;

        log.debug("Generating slots: shift={}-{}, break={}-{}",
                currentTime, shiftEnd, breakStart, breakEnd);

        while (!currentTime.plusMinutes(SLOT_DURATION_MINUTES).isAfter(shiftEnd)) {

            if (breakStart != null
                    && !currentTime.isBefore(breakStart)
                    && currentTime.isBefore(breakEnd)) {

                log.debug("Long break window reached at {}. Jumping to {}", currentTime, breakEnd);
                currentTime = breakEnd;
                slotCount = 0;
                continue;
            }

            if (breakStart != null
                    && currentTime.isBefore(breakStart)
                    && currentTime.plusMinutes(SLOT_DURATION_MINUTES).isAfter(breakStart)) {

                log.debug("Slot at {} would straddle break start {}. Jumping to {}",
                        currentTime, breakStart, breakEnd);
                currentTime = breakEnd;
                slotCount = 0;
                continue;
            }

            DoctorSlot slot = new DoctorSlot();
            slot.setDoctorId(availability.getDoctorId());
            slot.setSlotDate(availability.getDate());
            slot.setStartTime(currentTime);
            slot.setBooked(false);
            slots.add(slot);

            log.debug("Slot created at {}", currentTime);

            currentTime = currentTime.plusMinutes(SLOT_DURATION_MINUTES);
            slotCount++;

            if (slotCount == SLOTS_BEFORE_BREAK) {
                log.debug("3 consecutive slots done. Inserting 30-min short break at {}",
                        currentTime);
                currentTime = currentTime.plusMinutes(SLOT_DURATION_MINUTES);
                slotCount = 0;
            }
        }

        slotRepository.saveAll(slots);
        return slots.size();
    }
}