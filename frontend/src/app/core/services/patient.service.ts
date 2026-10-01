import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Patient } from '../../shared/models/patient.model';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly base = `${environment.gatewayUrl}/patient-service/api/v1/patients`;

  constructor(private http: HttpClient) {}

  getAllPatients(): Observable<Patient[]> {
    return this.http.get<Patient[]>(this.base);
  }

  getPatientById(patientId: number): Observable<Patient> {
    return this.http.get<Patient>(`${this.base}/${patientId}`);
  }

  updatePatient(patientId: number, data: Partial<Patient>): Observable<Patient> {
    return this.http.patch<Patient>(`${this.base}/${patientId}`, data);
  }

  deletePatient(patientId: number): Observable<string> {
    return this.http.delete(`${this.base}/${patientId}`, { responseType: 'text' });
  }
}
