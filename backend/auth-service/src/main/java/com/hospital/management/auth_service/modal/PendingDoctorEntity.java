package com.hospital.management.auth_service.modal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pending_doctors")
public class PendingDoctorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    private String name;
    private String email;
    private String contactDetails;
    private String specialization;
    private String designation;
}