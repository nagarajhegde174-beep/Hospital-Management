import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Appointment } from '../../shared/models/appointment.model';
import { MedicalHistory } from '../../shared/models/medical-history.model';
import { TimeSlot } from '../../shared/models/time-slot.model';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private readonly scheduleBase = `${environment.gatewayUrl}/doctor-service/api/v1/doctor-schedule`;
  private readonly apptBase = `${environment.gatewayUrl}/appointment-service/api/v1/appointments`;

  constructor(private http: HttpClient) {}

  getAvailableDates(doctorId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.scheduleBase}/available-dates/${doctorId}`);
  }

  getAvailableSlots(doctorId: number, date: string): Observable<TimeSlot[]> {
    return this.http.get<TimeSlot[]>(`${this.scheduleBase}/slots/doctor/${doctorId}/${date}`);
  }

  bookAppointment(patientId: number, doctorId: number, startTime: string): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.apptBase}/booking`, {
      patientId,
      doctorId,
      startTime
    });
  }

  getPatientAppointments(patientId: number): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.apptBase}`, {
      params: { patientId: patientId.toString() }
    });
  }

  getDoctorAppointments(doctorId: number): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.apptBase}`, {
      params: { doctorId: doctorId.toString() }
    });
  }

  cancelAppointment(confirmationCode: string): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.apptBase}/cancel/${confirmationCode}`, {});
  }

  rescheduleAppointment(confirmationCode: string, newAppointmentTime: string): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.apptBase}/reschedule`, {
      confirmationCode,
      newAppointmentTime
    });
  }

  completeAppointment(confirmationCode: string): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.apptBase}/complete/${confirmationCode}`, {});
  }

  getAllAppointments(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${this.apptBase}/all`);
  }

  getPatientHistoryForAppointment(confirmationCode: string): Observable<MedicalHistory[]> {
    return this.http.get<MedicalHistory[]>(`${this.apptBase}/${confirmationCode}/patient-history`);
  }
}
