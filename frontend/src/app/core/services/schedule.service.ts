import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TimeSlot } from '../../shared/models/time-slot.model';

export interface SetAvailabilityRequest {
  doctorId: number;
  date: string;         // YYYY-MM-DD
  shiftStart: string;   // HH:mm
  shiftEnd: string;     // HH:mm
  breakStart?: string;  // HH:mm (optional)
}

export interface MedicalHistoryEntry {
  recordId: number;
  patientId: number;
  diagnosis: string;
  diagnosedAt: string;
  prescribedMeds: string[];
}

@Injectable({ providedIn: 'root' })
export class ScheduleService {
  private readonly base = `${environment.gatewayUrl}/doctor-service/api/v1/doctor-schedule`;

  constructor(private http: HttpClient) {}

  setAvailability(payload: SetAvailabilityRequest): Observable<string> {
    return this.http.post(`${this.base}/set-availability`, payload, { responseType: 'text' });
  }

  getAvailableDates(doctorId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/available-dates/${doctorId}`);
  }

  getSlots(doctorId: number, date: string): Observable<TimeSlot[]> {
    return this.http.get<TimeSlot[]>(`${this.base}/slots/doctor/${doctorId}/${date}`);
  }

  addPrescription(slotId: number, diagnosis: string, prescribedMeds: string[]): Observable<MedicalHistoryEntry> {
    return this.http.post<MedicalHistoryEntry>(`${this.base}/add-prescription/${slotId}`, {
      diagnosis,
      prescribedMeds
    });
  }

  viewPatientHistory(slotId: number): Observable<MedicalHistoryEntry[]> {
    return this.http.get<MedicalHistoryEntry[]>(`${this.base}/view-history/${slotId}`);
  }
}
