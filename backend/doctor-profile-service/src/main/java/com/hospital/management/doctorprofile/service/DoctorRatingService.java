package com.hospital.management.doctorprofile.service;

import com.hospital.management.doctorprofile.dto.RatingRequestDTO;
import com.hospital.management.doctorprofile.dto.RatingResponseDTO;
import com.hospital.management.doctorprofile.entity.DoctorRating;
import com.hospital.management.doctorprofile.repository.DoctorRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorRatingService {

    private final DoctorRatingRepository ratingRepository;

    @Transactional
    public RatingResponseDTO submitRating(RatingRequestDTO request) {
        if (ratingRepository.existsByAppointmentId(request.getAppointmentId())) {
            throw new IllegalStateException("Rating already submitted for this appointment.");
        }

        DoctorRating rating = DoctorRating.builder()
                .doctorId(request.getDoctorId())
                .patientId(request.getPatientId())
                .appointmentId(request.getAppointmentId())
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        DoctorRating saved = ratingRepository.save(rating);
        log.info("Rating submitted for doctorId={} by patientId={}", request.getDoctorId(), request.getPatientId());
        return toDTO(saved);
    }

    public Double getAverageRating(Long doctorId) {
        Double avg = ratingRepository.findAverageRatingByDoctorId(doctorId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : null;
    }

    public boolean hasRated(Long appointmentId) {
        return ratingRepository.existsByAppointmentId(appointmentId);
    }

    private RatingResponseDTO toDTO(DoctorRating r) {
        return RatingResponseDTO.builder()
                .id(r.getId())
                .doctorId(r.getDoctorId())
                .patientId(r.getPatientId())
                .appointmentId(r.getAppointmentId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}