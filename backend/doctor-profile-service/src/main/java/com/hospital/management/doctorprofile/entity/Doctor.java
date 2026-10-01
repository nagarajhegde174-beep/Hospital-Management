package com.hospital.management.doctorprofile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a doctor's profile in the hospital system.
 * This service owns doctor identity and profile data only.
 * Scheduling is handled by the Doctor Schedule Service (Module 2.3).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String name;


    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String contactDetails;


    @Column(nullable = false)
    private String specialization;


    @Column(nullable = false)
    private String designation;
}
