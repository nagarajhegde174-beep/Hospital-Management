package com.hospital.management.doctorservice.repository;
import com.hospital.management.doctorservice.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    Optional<DoctorAvailability> findByDoctorIdAndDate(Long doctorId, LocalDate date);

    void deleteByDateBefore(LocalDate date);

    List<DoctorAvailability> findByDoctorIdAndDateGreaterThanEqualAndIsAvailableTrue(
            Long doctorId, LocalDate fromDate);
}