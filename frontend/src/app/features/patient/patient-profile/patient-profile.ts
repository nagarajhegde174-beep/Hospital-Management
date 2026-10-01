import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, timeout } from 'rxjs/operators';
import { Patient } from '../../../shared/models/patient.model';
import { PatientService } from '../../../core/services/patient.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ErrorService } from '../../../core/error/error.service';
import { UserProfileService } from '../../../core/services/user-profile.service';

@Component({
  selector: 'app-patient-profile',
  standalone: false,
  templateUrl: './patient-profile.html',
  styleUrl: './patient-profile.css'
})
export class PatientProfile implements OnInit, OnDestroy {
  profile: Patient | null = null;
  editMode = false;
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';
  private timers: ReturnType<typeof setTimeout>[] = [];

  form = {
    name: '',
    dateOfBirth: '',
    gender: '' as 'MALE' | 'FEMALE' | 'OTHER',
    contactDetails: ''
  };

  constructor(
    private patientService: PatientService,
    private auth: AuthService,
    private errorService: ErrorService,
    private cdr: ChangeDetectorRef,
    private userProfile: UserProfileService
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  ngOnDestroy(): void {
    this.timers.forEach(t => clearTimeout(t));
  }

  private loadProfile(): void {
    const id = this.auth.getServiceId();
    if (!id) return;

    this.loading = true;
    this.cdr.detectChanges();

    this.patientService.getPatientById(id).pipe(
      timeout(8000),
      finalize(() => { this.loading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: (p) => {
        this.profile = p;
        this.resetForm(p);
      },
      error: (err: any) => {
        if (err?.name === 'TimeoutError') {
          this.errorMessage = 'Request timed out. Please check the backend services are running.';
        } else {
          this.errorMessage = this.errorService.handleHttpError(err as HttpErrorResponse);
        }
        this.cdr.detectChanges();
      }
    });
  }

  private resetForm(p: Patient): void {
    this.form = {
      name: p.name,
      dateOfBirth: p.dateOfBirth,
      gender: p.gender,
      contactDetails: p.contactDetails
    };
  }

  startEdit(): void {
    this.editMode = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  cancelEdit(): void {
    this.editMode = false;
    if (this.profile) this.resetForm(this.profile);
  }

  saveProfile(): void {
    const id = this.auth.getServiceId();
    if (!id) return;

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.patientService.updatePatient(id, this.form).subscribe({
      next: (updated) => {
        this.saving = false;
        this.editMode = false;
        this.profile = updated;
        this.userProfile.setName(updated.name);
        this.successMessage = 'Profile updated successfully.';
        this.cdr.detectChanges();
        this.timers.push(setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000));
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.errorMessage = this.errorService.handleHttpError(err);
        this.cdr.detectChanges();
      }
    });
  }
}
