package com.hospital.management.doctorservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
@Table(
    name = "doctor_availability",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_availability_doctor_date",
        columnNames = {"doctor_id", "date"}
    )
)
public class DoctorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime shiftStart;

    @Column(nullable = false)
    private LocalTime shiftEnd;

    @Column(nullable = true)
    private LocalTime breakStart;

    private boolean isAvailable = true;

    @Column(nullable = false)
    private boolean isLocked = false;
}