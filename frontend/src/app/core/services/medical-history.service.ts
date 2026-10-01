import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MedicalHistory } from '../../shared/models/medical-history.model';

export interface MedicalHistoryRequest {
  patientId: number;
  doctorId?: number;
  diagnosis: string;
  diagnosedAt?: string;
  prescribedMeds: string[];
}

export interface MedicalHistoryUpdateRequest {
  diagnosis: string;
  prescribedMeds: string[];
}

@Injectable({ providedIn: 'root' })
export class MedicalHistoryService {
  private readonly base = `${environment.gatewayUrl}/medical-history-service/api/v1`;

  constructor(private http: HttpClient) {}

  getPatientHistory(patientId: number): Observable<MedicalHistory[]> {
    return this.http.get<MedicalHistory[]>(`${this.base}/patient/${patientId}`);
  }

  addRecord(payload: MedicalHistoryRequest): Observable<MedicalHistory> {
    return this.http.post<MedicalHistory>(`${this.base}`, payload);
  }

  updateRecord(recordId: number, payload: MedicalHistoryUpdateRequest): Observable<MedicalHistory> {
    return this.http.put<MedicalHistory>(`${this.base}/${recordId}`, payload);
  }

  deleteRecord(recordId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${recordId}`);
  }
}
