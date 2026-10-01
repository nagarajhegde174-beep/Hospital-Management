package com.hospital.management.medicalhistoryservice.repository;


import com.hospital.management.medicalhistoryservice.model.MedicalHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory,Long> {
    List<MedicalHistory> findByPatientId(Long patientId, Sort sort);
    Page<MedicalHistory> findByPatientId(Long patientId, Pageable pageable);
    boolean existsByPatientIdAndDiagnosis(Long id,String diagnosis);
}
