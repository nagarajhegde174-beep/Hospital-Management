import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize, timeout } from 'rxjs/operators';
import { Doctor } from '../../../shared/models/doctor.model';
import { DoctorService } from '../../../core/services/doctor.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ErrorService } from '../../../core/error/error.service';
import { UserProfileService } from '../../../core/services/user-profile.service';

@Component({
  selector: 'app-doctor-profile',
  standalone: false,
  templateUrl: './doctor-profile.html',
  styleUrl: './doctor-profile.css'
})
export class DoctorProfile implements OnInit, OnDestroy {
  profile: Doctor | null = null;
  editMode = false;
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';
  private timers: ReturnType<typeof setTimeout>[] = [];

  form = {
    name: '',
    specialization: '',
    designation: '',
    contactDetails: ''
  };

  constructor(
    private doctorService: DoctorService,
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

    this.doctorService.getDoctorById(id).pipe(
      timeout(8000),
      finalize(() => { this.loading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: (d) => {
        this.profile = d;
        this.resetForm(d);
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

  private resetForm(d: Doctor): void {
    this.form = {
      name: d.name,
      specialization: d.specialization,
      designation: d.designation,
      contactDetails: d.contactDetails
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

    this.doctorService.updateDoctor(id, this.form).subscribe({
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
