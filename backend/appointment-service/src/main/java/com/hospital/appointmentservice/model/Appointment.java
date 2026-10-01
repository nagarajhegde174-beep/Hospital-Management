package com.hospital.appointmentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointment", indexes = {
    @Index(name = "idx_appt_patient_id", columnList = "patient_id"),
    @Index(name = "idx_appt_doctor_id",  columnList = "doctor_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @SequenceGenerator(
            name = "appointment_seq_gen",
            sequenceName = "appointment_id_seq",
            initialValue = 1000,
            allocationSize = 1
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "appointment_seq_gen")
    private Long appointmentId;

    @Column(name = "confirmation_code", unique = true, nullable = false, length = 36)
    private String confirmationCode;

    @NotNull(message = "Patient ID is required")
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @NotNull(message = "Appointment date is required")
    @Column(nullable = false)
    private LocalDateTime appointmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED; // Default status

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Integer version;

    @PrePersist
    public void generateConfirmationCode() {
        if (this.confirmationCode == null) {
            this.confirmationCode = UUID.randomUUID().toString();
        }
    }
}