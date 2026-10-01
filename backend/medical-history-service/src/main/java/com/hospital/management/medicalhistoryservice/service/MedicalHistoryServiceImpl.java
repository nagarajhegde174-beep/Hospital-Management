package com.hospital.management.medicalhistoryservice.service;

import com.hospital.management.medicalhistoryservice.DTO.MedicalHistoryRequestDTO;
import com.hospital.management.medicalhistoryservice.DTO.MedicalHistoryUpdateDTO;
import com.hospital.management.medicalhistoryservice.client.PatientClient;
import com.hospital.management.medicalhistoryservice.exception.PatientNotFoundException;
import com.hospital.management.medicalhistoryservice.exception.ResourceNotFoundException;
import com.hospital.management.medicalhistoryservice.model.MedicalHistory;
import com.hospital.management.medicalhistoryservice.repository.MedicalHistoryRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalHistoryServiceImpl implements MedicalHistoryService {

    private final PatientClient patientClient;
    private final MedicalHistoryRepository medicalHistoryRepository;

    private MedicalHistory getEntityById(Long recordId) {
        return medicalHistoryRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("No medical record with Id: " + recordId));
    }

    @Transactional
    public MedicalHistory addMedicalHistory(MedicalHistoryRequestDTO dto){
        try{
            patientClient.getPatientById(dto.getPatientId());
        } catch(FeignException.NotFound e){
            throw new PatientNotFoundException("No patient found with id " + dto.getPatientId());
        } catch (Exception e){
            log.error("Patient service call failed. Cause: {}", e.getClass().getName() + " - " + e.getMessage(), e);
            throw new RuntimeException("Unexpected error while contacting patient service");
        }
        MedicalHistory medicalHistory = new MedicalHistory();
        medicalHistory.setPatientId(dto.getPatientId());
        medicalHistory.setDoctorId(dto.getDoctorId());
        medicalHistory.setDiagnosis(dto.getDiagnosis().toLowerCase());
        medicalHistory.setDiagnosedAt(dto.getDiagnosedAt() != null ? dto.getDiagnosedAt() : LocalDate.now());
        medicalHistory.setPrescribedMeds(medsToLowerCase(dto.getPrescribedMeds()));
        return medicalHistoryRepository.save(medicalHistory);
    }

    public MedicalHistory getMedicalHistoryById(Long recordId){
        return getEntityById(recordId);
    }

    @Transactional
    public MedicalHistory updateMedicalHistory(Long recordId, MedicalHistoryUpdateDTO dto){
        MedicalHistory medicalHistory = medicalHistoryRepository.findById(recordId).orElseThrow(
                ()->new ResourceNotFoundException("Record not found with id: " + recordId)
        );
        medicalHistory.setDiagnosis(dto.getDiagnosis().toLowerCase());
        medicalHistory.setPrescribedMeds(medsToLowerCase(dto.getPrescribedMeds() != null ? dto.getPrescribedMeds() : new ArrayList<>()));
        return medicalHistoryRepository.save(medicalHistory);
    }
    @Transactional
    public String deleteMedicalHistory(Long recordId){
        MedicalHistory medicalHistory = getEntityById(recordId);
        medicalHistoryRepository.delete(medicalHistory);
        return "Successfully removed the medical record with record id :" + recordId;
    }

    private List<String> medsToLowerCase(List<String> meds){
        if (meds == null) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for(String med : meds){
            result.add(med.toLowerCase());
        }
        return result;
    }

    public Page<MedicalHistory> getMedicalHistoryByPatientId(Long patientId,int pageNumber,int pageSize,String order){
        if (order == null) throw new IllegalArgumentException("Sort order must not be null");
        Sort sort;
        if(order.equalsIgnoreCase("asc")){
            sort = Sort.by(Sort.Direction.ASC,"diagnosedAt");
        }else if(order.equalsIgnoreCase("desc")){
            sort = Sort.by(Sort.Direction.DESC,"diagnosedAt");
        } else{
            throw new IllegalArgumentException("Invalid sort parameter: " + order +". Allowed values are 'asc' or 'desc'");
        }
        Pageable pageable = PageRequest.of(pageNumber,pageSize, sort);
        return medicalHistoryRepository.findByPatientId(patientId, pageable);
    }

    public List<MedicalHistory> getMedicalHistoryByPatientId(Long patientId){
        Sort sort = Sort.by(Sort.Direction.DESC,"diagnosedAt");
        return medicalHistoryRepository.findByPatientId(patientId, sort);
    }

    public Page<MedicalHistory> getAllMedicalHistories(int pageNumber,int pageSize,String order,String columnName){
        Sort sort;

        if(columnName.equalsIgnoreCase("diagnosis")){
            columnName = "diagnosis";
        } else if (columnName.equalsIgnoreCase("diagnosedAt")){
            columnName = "diagnosedAt";
        } else {
            throw new IllegalArgumentException("Invalid column parameter: " + columnName + ". Expected diagnosis or diagnosedAt");
        }
        if(order.equalsIgnoreCase("asc")){
            sort = Sort.by(Sort.Direction.ASC,columnName);
        } else if(order.equalsIgnoreCase("desc")){
            sort = Sort.by(Sort.Direction.DESC,columnName);
        } else{
            throw new IllegalArgumentException("Invalid sort parameter: " + order +". Allowed values are 'asc' or 'desc'");
        }
        Pageable pageable = PageRequest.of(pageNumber,pageSize,sort);
        return medicalHistoryRepository.findAll(pageable);
    }

}