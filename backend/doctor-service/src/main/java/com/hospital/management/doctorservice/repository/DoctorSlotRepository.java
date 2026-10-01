package com.hospital.management.doctorservice.repository;
import com.hospital.management.doctorservice.entity.DoctorSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, Long> {

    List<DoctorSlot> findByDoctorIdAndSlotDate(Long doctorId, LocalDate slotDate);

    void deleteBySlotDateBefore(LocalDate date);

    Optional<DoctorSlot> findByDoctorIdAndSlotDateAndStartTime(
            Long doctorId, LocalDate slotDate, LocalTime startTime);
}