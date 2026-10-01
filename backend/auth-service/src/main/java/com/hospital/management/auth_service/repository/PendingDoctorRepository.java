package com.hospital.management.auth_service.repository;

import com.hospital.management.auth_service.modal.PendingDoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PendingDoctorRepository extends JpaRepository<PendingDoctorEntity, Long> {
    Optional<PendingDoctorEntity> findByUserId(Long userId);
}