package com.hospital.management.doctorservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Data
@Table(
    name = "doctor_slot",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_slot_doctor_date_time",
        columnNames = {"doctor_id", "slot_date", "start_time"}
    )
)
public class DoctorSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long slotId;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private boolean isBooked = false;

    private Long patientId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}