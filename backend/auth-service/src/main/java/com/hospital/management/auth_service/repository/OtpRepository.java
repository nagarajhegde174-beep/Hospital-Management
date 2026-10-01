package com.hospital.management.auth_service.repository;

import com.hospital.management.auth_service.modal.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    Optional<OtpEntity> findByEmailAndUsed(String email, boolean used);
    void deleteByEmail(String email);
}