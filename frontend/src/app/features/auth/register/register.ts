import { ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, RegisterRequest } from '../../../core/auth/auth.service';
import { ErrorService } from '../../../core/error/error.service';
import { ToastService } from '../../../core/services/toast.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-register',
  standalone: false,
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register implements OnDestroy {
  role: 'PATIENT' | 'DOCTOR' = 'PATIENT';

  name = '';
  email = '';
  password = '';
  confirmPassword = '';
  showPassword = false;
  showConfirm = false;
  contactDetails = '';

  // Patient-only
  dateOfBirth = '';
  gender = '';

  // Doctor-only
  specialization = '';
  designation = '';

  loading = false;
  errorMessage = '';
  successMessage = '';
  private redirectTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private auth: AuthService,
    private errorService: ErrorService,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnDestroy(): void {
    if (this.redirectTimer) clearTimeout(this.redirectTimer);
  }

  get isPatient(): boolean { return this.role === 'PATIENT'; }
  get isDoctor(): boolean { return this.role === 'DOCTOR'; }

  get passwordsMatch(): boolean {
    return this.password === this.confirmPassword;
  }

  switchRole(r: 'PATIENT' | 'DOCTOR'): void {
    this.role = r;
    this.errorMessage = '';
  }
  get maxDob(): string {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    return yesterday.toISOString().split('T')[0];
  }

  onSubmit(form: NgForm): void {
    if (form.invalid) {
      form.form.markAllAsTouched();
      return;
    }
    if (!this.passwordsMatch) return;

    if (this.isPatient && this.dateOfBirth) {
      const dob = new Date(this.dateOfBirth);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (dob >= today) {
        this.errorMessage = 'Date of birth cannot be today or a future date.';
        this.cdr.detectChanges();
        return;
      }
    }

    this.loading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    const payload: RegisterRequest = {
      name: this.name,
      email: this.email,
      password: this.password,
      role: this.role,
      contactDetails: this.contactDetails
    };

    if (this.isPatient) {
      payload.dateOfBirth = this.dateOfBirth;
      payload.gender = this.gender;
    } else {
      payload.specialization = this.specialization;
      payload.designation = this.designation;
    }

    this.auth.register(payload).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = `${this.role === 'DOCTOR' ? 'Doctor account' : 'Account'} created! Redirecting to sign in…`;
        this.toast.success(this.successMessage);
        this.cdr.detectChanges();
        this.redirectTimer = setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage = this.errorService.handleHttpError(err);
        this.toast.error(this.errorMessage);
        this.cdr.detectChanges();
      }
    });
  }
}
