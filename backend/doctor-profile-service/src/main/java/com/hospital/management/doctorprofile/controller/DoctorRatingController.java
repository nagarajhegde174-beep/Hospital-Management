package com.hospital.management.doctorprofile.controller;

import com.hospital.management.doctorprofile.dto.RatingRequestDTO;
import com.hospital.management.doctorprofile.dto.RatingResponseDTO;
import com.hospital.management.doctorprofile.repository.DoctorRatingRepository;
import com.hospital.management.doctorprofile.service.DoctorRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class DoctorRatingController {

    private final DoctorRatingService ratingService;
    private final DoctorRatingRepository ratingRepository;
    @PostMapping
    public ResponseEntity<RatingResponseDTO> submitRating(
            @Valid @RequestBody RatingRequestDTO request) {
        return ResponseEntity.ok(ratingService.submitRating(request));
    }

    @GetMapping("/doctor/{doctorId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long doctorId) {
        Double avg = ratingService.getAverageRating(doctorId);
        return avg != null ? ResponseEntity.ok(avg) : ResponseEntity.noContent().build();
    }

    @GetMapping("/appointment/{appointmentId}/rated")
    public ResponseEntity<Boolean> hasRated(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(ratingService.hasRated(appointmentId));
    }

    @GetMapping("/appointment/{appointmentId}/rating")
    public ResponseEntity<Integer> getRatingByAppointment(@PathVariable Long appointmentId) {
        return ratingRepository.findByAppointmentId(appointmentId)
                .map(r -> ResponseEntity.ok(r.getRating()))
                .orElse(ResponseEntity.ok(0));
    }
}