package com.hospital.management.medicalhistoryservice.service;

import com.hospital.management.medicalhistoryservice.DTO.MedicalHistoryUpdateDTO;
import com.hospital.management.medicalhistoryservice.exception.PatientNotFoundException;
import com.hospital.management.medicalhistoryservice.exception.ResourceNotFoundException;
import com.hospital.management.medicalhistoryservice.repository.MedicalHistoryRepository;
import com.hospital.management.medicalhistoryservice.DTO.Mapperdto;
import com.hospital.management.medicalhistoryservice.DTO.MedicalHistoryRequestDTO;
import com.hospital.management.medicalhistoryservice.client.PatientClient;
import com.hospital.management.medicalhistoryservice.model.MedicalHistory;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalHistoryServiceTest {

    @Mock
    private PatientClient patientClient;

    @Mock
    private MedicalHistoryRepository medicalHistoryRepository;

    @Mock
    private Mapperdto mapper;

    @InjectMocks
    private MedicalHistoryServiceImpl medicalHistoryService;

    private MedicalHistoryRequestDTO requestDTO;
    private MedicalHistory savedMedicalHistory;

    @BeforeEach
    void setUp() {
        requestDTO = new MedicalHistoryRequestDTO();
        requestDTO.setPatientId(1L);
        requestDTO.setDiagnosis("FEVER");
        requestDTO.setDiagnosedAt(LocalDate.of(2026, 1, 15));
        requestDTO.setPrescribedMeds(Arrays.asList("PARACETAMOL", "IBUPROFEN"));

        savedMedicalHistory = new MedicalHistory();
        savedMedicalHistory.setPatientId(1L);
        savedMedicalHistory.setDiagnosis("fever");
        savedMedicalHistory.setDiagnosedAt(LocalDate.of(2026, 1, 15));
        savedMedicalHistory.setPrescribedMeds(Arrays.asList("paracetamol", "ibuprofen"));
    }

    // -------------------- addMedicalHistory --------------------

    @Test
    void addMedicalHistory_whenValidRequest_shouldSaveAndReturnMedicalHistory() {
        when(medicalHistoryRepository.save(any(MedicalHistory.class)))
                .thenReturn(savedMedicalHistory);

        MedicalHistory result = medicalHistoryService.addMedicalHistory(requestDTO);

        assertNotNull(result);
        assertEquals(1L, result.getPatientId());
        assertEquals("fever", result.getDiagnosis());
        assertEquals(Arrays.asList("paracetamol", "ibuprofen"), result.getPrescribedMeds());
        verify(patientClient, times(1)).getPatientById(1L);
        verify(medicalHistoryRepository, times(1)).save(any(MedicalHistory.class));
    }

    @Test
    void addMedicalHistory_whenPatientNotFound_shouldThrowPatientNotFoundException() {
        when(patientClient.getPatientById(1L))
                .thenThrow(mock(FeignException.NotFound.class));

        assertThrows(
                PatientNotFoundException.class,
                () -> medicalHistoryService.addMedicalHistory(requestDTO)
        );
        verify(medicalHistoryRepository, never()).save(any(MedicalHistory.class));
    }

    // -------------------- updateMedicalHistory --------------------

    @Test
    void updateMedicalHistory_whenValidRequestAndDiagnosedToday_shouldUpdateAndReturn() {
        MedicalHistory existing = new MedicalHistory();
        existing.setRecordId(10L);
        existing.setPatientId(1L);
        existing.setDiagnosis("fever");
        existing.setDiagnosedAt(LocalDate.now());
        existing.setPrescribedMeds(new ArrayList<>(Arrays.asList("paracetamol")));

        MedicalHistoryUpdateDTO updateDTO = new MedicalHistoryUpdateDTO();
        updateDTO.setDiagnosis("FLU");
        updateDTO.setPrescribedMeds(Arrays.asList("OSELTAMIVIR", "IBUPROFEN"));

        when(medicalHistoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(medicalHistoryRepository.save(any(MedicalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicalHistory result = medicalHistoryService.updateMedicalHistory(10L, updateDTO);

        assertNotNull(result);
        assertEquals("flu", result.getDiagnosis());
        assertEquals(Arrays.asList("oseltamivir", "ibuprofen"), result.getPrescribedMeds());
        verify(medicalHistoryRepository, times(1)).findById(10L);
        verify(medicalHistoryRepository, times(1)).save(existing);
    }

    @Test
    void updateMedicalHistory_whenRecordNotFound_shouldThrowResourceNotFoundException() {
        MedicalHistoryUpdateDTO updateDTO = new MedicalHistoryUpdateDTO();
        updateDTO.setDiagnosis("flu");
        updateDTO.setPrescribedMeds(Arrays.asList("oseltamivir"));

        when(medicalHistoryRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> medicalHistoryService.updateMedicalHistory(99L, updateDTO)
        );
        assertEquals("Record not found with id: 99", ex.getMessage());
        verify(medicalHistoryRepository, never()).save(any(MedicalHistory.class));
    }

    @Test
    void updateMedicalHistory_whenRecordDiagnosedBeforeToday_shouldUpdateSuccessfully() {
        // Service has no date restriction — past-dated records can still be updated
        MedicalHistory existing = new MedicalHistory();
        existing.setRecordId(10L);
        existing.setPatientId(1L);
        existing.setDiagnosis("fever");
        existing.setDiagnosedAt(LocalDate.now().minusDays(1));
        existing.setPrescribedMeds(new ArrayList<>(Arrays.asList("paracetamol")));

        MedicalHistoryUpdateDTO updateDTO = new MedicalHistoryUpdateDTO();
        updateDTO.setDiagnosis("flu");
        updateDTO.setPrescribedMeds(Arrays.asList("oseltamivir"));

        when(medicalHistoryRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(medicalHistoryRepository.save(any(MedicalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicalHistory result = medicalHistoryService.updateMedicalHistory(10L, updateDTO);

        assertNotNull(result);
        assertEquals("flu", result.getDiagnosis());
        assertEquals(Arrays.asList("oseltamivir"), result.getPrescribedMeds());
        verify(medicalHistoryRepository, times(1)).findById(10L);
        verify(medicalHistoryRepository, times(1)).save(existing);
    }

    // -------------------- getMedicalHistoryByPatientId (paginated) --------------------

    @Test
    void getMedicalHistoryByPatientIdPaginated_whenOrderAsc_shouldReturnPageSortedAsc() {
        MedicalHistory record1 = new MedicalHistory();
        record1.setPatientId(1L);
        record1.setDiagnosis("fever");
        record1.setDiagnosedAt(LocalDate.of(2026, 1, 10));

        MedicalHistory record2 = new MedicalHistory();
        record2.setPatientId(1L);
        record2.setDiagnosis("flu");
        record2.setDiagnosedAt(LocalDate.of(2026, 2, 20));

        Page<MedicalHistory> page = new PageImpl<>(Arrays.asList(record1, record2));
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "diagnosedAt"));

        when(medicalHistoryRepository.findByPatientId(eq(1L), eq(expectedPageable)))
                .thenReturn(page);

        Page<MedicalHistory> result = medicalHistoryService.getMedicalHistoryByPatientId(1L, 0, 10, "asc");

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(medicalHistoryRepository, times(1)).findByPatientId(1L, expectedPageable);
    }

    @Test
    void getMedicalHistoryByPatientIdPaginated_whenOrderDesc_shouldReturnPageSortedDesc() {
        Page<MedicalHistory> page = new PageImpl<>(Collections.singletonList(savedMedicalHistory));
        Pageable expectedPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "diagnosedAt"));

        when(medicalHistoryRepository.findByPatientId(eq(1L), eq(expectedPageable)))
                .thenReturn(page);

        Page<MedicalHistory> result = medicalHistoryService.getMedicalHistoryByPatientId(1L, 0, 5, "desc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(medicalHistoryRepository, times(1)).findByPatientId(1L, expectedPageable);
    }

    @Test
    void getMedicalHistoryByPatientIdPaginated_whenInvalidOrder_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> medicalHistoryService.getMedicalHistoryByPatientId(1L, 0, 10, "random")
        );
        assertEquals("Invalid sort parameter: random. Allowed values are 'asc' or 'desc'", ex.getMessage());
        verify(medicalHistoryRepository, never()).findByPatientId(anyLong(), any(Pageable.class));
    }

    @Test
    void getMedicalHistoryByPatientIdPaginated_whenNoRecordsFound_shouldReturnEmptyPage() {
        // Service does not throw on empty — it returns whatever the repository returns
        Page<MedicalHistory> emptyPage = new PageImpl<>(Collections.emptyList());
        when(medicalHistoryRepository.findByPatientId(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<MedicalHistory> result = medicalHistoryService.getMedicalHistoryByPatientId(1L, 0, 10, "asc");

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        verify(medicalHistoryRepository, times(1)).findByPatientId(eq(1L), any(Pageable.class));
    }

    // -------------------- getMedicalHistoryByPatientId (non-paginated list) --------------------

    @Test
    void getMedicalHistoryByPatientId_whenRecordsExist_shouldReturnListSortedDesc() {
        MedicalHistory record1 = new MedicalHistory();
        record1.setPatientId(1L);
        record1.setDiagnosis("fever");
        record1.setDiagnosedAt(LocalDate.of(2026, 2, 20));

        MedicalHistory record2 = new MedicalHistory();
        record2.setPatientId(1L);
        record2.setDiagnosis("flu");
        record2.setDiagnosedAt(LocalDate.of(2026, 1, 10));

        List<MedicalHistory> records = Arrays.asList(record1, record2);
        Sort expectedSort = Sort.by(Sort.Direction.DESC, "diagnosedAt");

        when(medicalHistoryRepository.findByPatientId(eq(1L), eq(expectedSort)))
                .thenReturn(records);

        List<MedicalHistory> result = medicalHistoryService.getMedicalHistoryByPatientId(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("fever", result.get(0).getDiagnosis());
        verify(medicalHistoryRepository, times(1)).findByPatientId(1L, expectedSort);
    }

    @Test
    void getMedicalHistoryByPatientId_whenListIsEmpty_shouldReturnEmptyList() {
        // Service does not throw on empty — it returns whatever the repository returns
        Sort expectedSort = Sort.by(Sort.Direction.DESC, "diagnosedAt");

        when(medicalHistoryRepository.findByPatientId(eq(1L), eq(expectedSort)))
                .thenReturn(Collections.emptyList());

        List<MedicalHistory> result = medicalHistoryService.getMedicalHistoryByPatientId(1L);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(medicalHistoryRepository, times(1)).findByPatientId(1L, expectedSort);
    }

    // -------------------- getAllMedicalHistories --------------------

    @Test
    void getAllMedicalHistories_whenSortByDiagnosisAsc_shouldReturnPage() {
        Page<MedicalHistory> page = new PageImpl<>(Collections.singletonList(savedMedicalHistory));
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "diagnosis"));

        when(medicalHistoryRepository.findAll(eq(expectedPageable))).thenReturn(page);

        Page<MedicalHistory> result = medicalHistoryService.getAllMedicalHistories(0, 10, "asc", "diagnosis");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(medicalHistoryRepository, times(1)).findAll(expectedPageable);
    }

    @Test
    void getAllMedicalHistories_whenSortByDiagnosisDesc_shouldReturnPage() {
        Page<MedicalHistory> page = new PageImpl<>(Collections.singletonList(savedMedicalHistory));
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "diagnosis"));

        when(medicalHistoryRepository.findAll(eq(expectedPageable))).thenReturn(page);

        Page<MedicalHistory> result = medicalHistoryService.getAllMedicalHistories(0, 10, "desc", "diagnosis");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(medicalHistoryRepository, times(1)).findAll(expectedPageable);
    }

    @Test
    void getAllMedicalHistories_whenInvalidOrder_shouldThrowIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> medicalHistoryService.getAllMedicalHistories(0, 10, "invalid", "diagnosis")
        );
        assertEquals("Invalid sort parameter: invalid. Allowed values are 'asc' or 'desc'", ex.getMessage());
        verify(medicalHistoryRepository, never()).findAll(any(Pageable.class));
    }
}