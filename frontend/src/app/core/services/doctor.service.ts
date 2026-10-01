import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Doctor } from '../../shared/models/doctor.model';

// Shape the specialization endpoint actually returns
interface DoctorSpecResponse {
  doctorId: number;
  doctorName: string;
  specialization: string;
  designation: string;
}

@Injectable({ providedIn: 'root' })
export class DoctorService {
  private readonly base=`${environment.gatewayUrl}/doctor-profile-service/api/v1/doctors`;
  private readonly rating_base = `${environment.gatewayUrl}/doctor-profile-service/api/v1/ratings`;

  constructor(private http: HttpClient) {}

  getAllDoctors(): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(this.base);
  }

  getDoctorsBySpecialization(specialization: string): Observable<Doctor[]> {
    // Backend returns {doctorId, doctorName, specialization} — map to Doctor interface
    return this.http.get<DoctorSpecResponse[]>(
      `${this.base}/specialization/${encodeURIComponent(specialization)}`
    ).pipe(
      map(docs => docs.map(d => ({
        id: d.doctorId,
        name: d.doctorName,
        specialization: d.specialization,
        designation: d.designation,
        email: '',
        contactDetails: ''
      } as Doctor)))
    );
  }

  getDoctorById(doctorId: number): Observable<Doctor> {
    return this.http.get<Doctor>(`${this.base}/${doctorId}`);
  }

  updateDoctor(doctorId: number, payload: Partial<Doctor>): Observable<Doctor> {
    return this.http.patch<Doctor>(`${this.base}/${doctorId}`, payload);
  }

  deleteDoctor(doctorId: number): Observable<string> {
    return this.http.delete(`${this.base}/${doctorId}`, { responseType: 'text' });
  }
  submitRating(payload: { doctorId: number; patientId: number; appointmentId: number; rating: number }): Observable<any> {
  return this.http.post(`${this.rating_base}`, payload);
}

getAverageRating(doctorId: number): Observable<number> {
  return this.http.get<number>(`${this.rating_base}/doctor/${doctorId}/average`);
}

hasRated(appointmentId: number): Observable<boolean> {
  return this.http.get<boolean>(`${this.rating_base}/appointment/${appointmentId}/rated`);
}
getRatingByAppointment(appointmentId: number): Observable<number> {
  return this.http.get<number>(`${this.rating_base}/appointment/${appointmentId}/rating`);
}
}
