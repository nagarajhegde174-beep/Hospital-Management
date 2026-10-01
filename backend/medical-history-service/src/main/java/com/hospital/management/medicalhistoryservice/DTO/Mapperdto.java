package com.hospital.management.medicalhistoryservice.DTO;

import com.hospital.management.medicalhistoryservice.model.MedicalHistory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Mapperdto {

    public MedicalHistoryResponseDTO mapToResponse(MedicalHistory medicalHistory){
        MedicalHistoryResponseDTO medicalHistoryResponseDTO =
                new MedicalHistoryResponseDTO.MedicalHistoryResponseDTOBuilder()
                        .diagnosis(medicalHistory.getDiagnosis())
                        .patientId(medicalHistory.getPatientId())
                        .doctorId(medicalHistory.getDoctorId())
                        .prescribedMeds(medicalHistory.getPrescribedMeds())
                        .diagnosedAt(medicalHistory.getDiagnosedAt())
                        .recordId(medicalHistory.getRecordId())
                        .build();

        return medicalHistoryResponseDTO;
    }
    public List<MedicalHistoryResponseDTO> mapToResponse(List<MedicalHistory> medicalHistories){
        return medicalHistories
                .stream()
                .map(medicalHistory ->
                        new MedicalHistoryResponseDTO.MedicalHistoryResponseDTOBuilder()
                                .recordId(medicalHistory.getRecordId())
                                .patientId(medicalHistory.getPatientId())
                                .doctorId(medicalHistory.getDoctorId())
                                .diagnosis(medicalHistory.getDiagnosis())
                                .prescribedMeds(medicalHistory.getPrescribedMeds())
                                .diagnosedAt(medicalHistory.getDiagnosedAt())
                                .build())
                .toList();
    }
    public Page<MedicalHistoryResponseDTO> mapToResponsePage(Page<MedicalHistory> medicalHistories){
        return medicalHistories
                .map(medicalHistory -> new MedicalHistoryResponseDTO
                                .MedicalHistoryResponseDTOBuilder()
                                .diagnosis(medicalHistory.getDiagnosis())
                                .diagnosedAt(medicalHistory.getDiagnosedAt())
                                .prescribedMeds(medicalHistory.getPrescribedMeds())
                                .patientId(medicalHistory.getPatientId())
                                .doctorId(medicalHistory.getDoctorId())
                                .recordId(medicalHistory.getRecordId())
                                .build()
                        );

    }
}
