package com.hospital.management.medicalhistoryservice.service;

import com.hospital.management.medicalhistoryservice.DTO.MedicalHistoryRequestDTO;
import com.hospital.management.medicalhistoryservice.DTO.MedicalHistoryUpdateDTO;
import com.hospital.management.medicalhistoryservice.model.MedicalHistory;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MedicalHistoryService {
    MedicalHistory addMedicalHistory(MedicalHistoryRequestDTO dto);
    MedicalHistory getMedicalHistoryById(Long id);
    MedicalHistory updateMedicalHistory(Long id, MedicalHistoryUpdateDTO dto);
    String deleteMedicalHistory(Long id);
    Page<MedicalHistory> getAllMedicalHistories(int pageNumber, int pageSize, String order, String columnName);
    Page<MedicalHistory> getMedicalHistoryByPatientId(Long patientId,int pageNumber,int pageSize,String order);
    List<MedicalHistory> getMedicalHistoryByPatientId(Long patientId);
}
