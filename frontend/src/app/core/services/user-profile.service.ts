import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { PatientService } from './patient.service';
import { DoctorService } from './doctor.service';

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  private nameSubject = new BehaviorSubject<string>('');
  readonly displayName$ = this.nameSubject.asObservable();
  private loaded = false;

  constructor(
    private auth: AuthService,
    private patientService: PatientService,
    private doctorService: DoctorService
  ) {
    this.auth.logout$.subscribe(() => {
      this.loaded = false;
      this.nameSubject.next('');
    });
  }

  setName(name: string): void {
    this.nameSubject.next(name);
    this.loaded = true;
  }
  resetLoaded(): void {
    this.loaded = false;
    this.nameSubject.next('');
  }

  loadName(): void {
    if (this.loaded) return;

    const role = this.auth.getRole();
    const id = this.auth.getServiceId();

    if (!id) {
      const email = this.auth.getUserName() ?? '';
      this.nameSubject.next(email.split('@')[0]);
      this.loaded = true;
      return;
    }

    if (role === 'ROLE_PATIENT') {
      this.patientService.getPatientById(id).subscribe({
        next: (p) => {
          this.nameSubject.next(p.name);
          this.loaded = true;
        },
        error: () => {
          const email = this.auth.getUserName() ?? '';
          this.nameSubject.next(email.split('@')[0]);
          this.loaded = true;
        }
      });
    } else if (role === 'ROLE_DOCTOR') {
      this.doctorService.getDoctorById(id).subscribe({
        next: (d) => {
          this.nameSubject.next(d.name);
          this.loaded = true;
        },
        error: () => {
          const email = this.auth.getUserName() ?? '';
          this.nameSubject.next(email.split('@')[0]);
          this.loaded = true;
        }
      });
    }
  }

}