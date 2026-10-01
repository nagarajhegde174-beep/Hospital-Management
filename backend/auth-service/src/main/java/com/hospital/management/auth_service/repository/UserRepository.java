package com.hospital.management.auth_service.repository;

import com.hospital.management.auth_service.modal.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<UserEntity> findByRole(String role);

    Optional<UserEntity> findFirstByServiceIdAndRole(Long serviceId, String role);

    Optional<UserEntity> findByUserId(Long userId);

    List<UserEntity> findByRoleAndStatus(String role, String status);
}