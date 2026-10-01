package com.hospital.management.doctorprofile.repository;

import com.hospital.management.doctorprofile.entity.DoctorRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface DoctorRatingRepository extends JpaRepository<DoctorRating, Long> {
    List<DoctorRating> findByDoctorId(Long doctorId);
    Optional<DoctorRating> findByAppointmentId(Long appointmentId);
    boolean existsByAppointmentId(Long appointmentId);

    @Query("SELECT AVG(r.rating) FROM DoctorRating r WHERE r.doctorId = :doctorId")
    Double findAverageRatingByDoctorId(Long doctorId);
}