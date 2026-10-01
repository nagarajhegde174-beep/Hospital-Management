package com.hospital.management.medicalhistoryservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "medical_history", indexes = {
    @Index(name = "idx_mh_patient_id", columnList = "patientId"),
    @Index(name = "idx_mh_doctor_id",  columnList = "doctorId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;
    @Column(nullable = false,length = 500)
    private String diagnosis;
    @Column(nullable = false)
    private LocalDate diagnosedAt;
    @Column(nullable = false)
    private Long patientId;
    @Column
    private Long doctorId;
    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(
            name = "prescribed_medications",
            joinColumns = @JoinColumn(name = "recordId")
    )
    private List<String> prescribedMeds;
    @Column(nullable = false , updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }
}
