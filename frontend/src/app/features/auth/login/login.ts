import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { NgForm } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, timeout } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import { ErrorService } from '../../../core/error/error.service';
import { UserProfileService } from '../../../core/services/user-profile.service';
import { ToastService } from '../../../core/services/toast.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login implements OnInit {
  email = '';
  password = '';
  showPassword = false;
  loading = false;
  errorMessage = '';
  infoMessage = '';
  showForgotModal = false;
  forgotEmail = '';
  forgotStep: 'email' | 'otp' | 'reset' = 'email';
  forgotOtp = '';
  newPassword = '';
  confirmPassword = '';
  forgotLoading = false;
  forgotSuccess = '';
  forgotError = '';


  constructor(
    private auth: AuthService,
    private errorService: ErrorService,
    private userProfile: UserProfileService,
    private toast: ToastService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    const reason = this.route.snapshot.queryParamMap.get('reason');
    if (reason === 'session-expired') {
      this.infoMessage = 'Your session has expired. Please sign in again.';
    } else if (reason === 'auth-required') {
      this.infoMessage = 'Please sign in to continue.';
    } else if (reason === 'forbidden') {
      this.infoMessage = 'You do not have access to that page.';
    }

    if (this.auth.isLoggedIn()) {
      this.redirectByRole();
    }
  }

  onSubmit(form: NgForm): void {
    if (form.invalid) {
      form.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.auth.login({ email: this.email, password: this.password }).pipe(
      timeout(8000),
      finalize(() => { this.loading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: () => {
        this.userProfile.loadName();
        this.toast.success('Signed in successfully.');
        this.redirectByRole();
      },
      error: (err: any) => {
        if (err?.name === 'TimeoutError') {
          this.errorMessage = 'Could not reach the auth server. Please ensure all backend services are running.';
        } else if (err?.status === 403) {
          const body = err?.error;
          const msg = typeof body === 'string' ? body.trim() : (body?.message ?? '').trim();
          this.errorMessage = msg || 'Your account is pending admin approval. Please wait for the administrator to approve your account.';
        } else if (err?.status === 401 || err?.status === 400) {
          const body = err?.error;
          const msg = typeof body === 'string' ? body.trim() : (body?.message ?? '').trim();
          if (msg.toLowerCase().includes('pending')) {
            this.errorMessage = 'Your account is pending admin approval. Please wait for the administrator to approve your account.';
          } else {
            this.errorMessage = msg || 'Invalid email or password.';
          }
        } else {
          this.errorMessage = this.errorService.handleHttpError(err as HttpErrorResponse);
        }
        this.cdr.detectChanges();
      }
    });
  }

  private redirectByRole(): void {
    const role = this.auth.getRole();
    if (role === 'ROLE_PATIENT') {
      this.router.navigate(['/patient/dashboard']);
    } else if (role === 'ROLE_DOCTOR') {
      this.router.navigate(['/doctor/dashboard']);
    } else if (role === 'ROLE_ADMIN') {
      this.router.navigate(['/admin/doctors']);
    } else {
      this.errorMessage = `Role "${role}" has no dashboard configured. Contact administrator.`;
    }
  }
  openForgotModal(): void {
    this.showForgotModal = true;
    this.forgotStep = 'email';
    this.forgotEmail = '';
    this.forgotOtp = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.forgotSuccess = '';
    this.forgotError = '';
  }

  closeForgotModal(): void {
    this.showForgotModal = false;
  }

  sendOtp(): void {
    if (!this.forgotEmail) return;
    this.forgotLoading = true;
    this.forgotError = '';
    this.auth.sendOtp(this.forgotEmail).pipe(
      timeout(8000),
      finalize(() => { this.forgotLoading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: () => {
        this.forgotStep = 'otp';
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        if (err?.name === 'TimeoutError') {
          this.forgotError = 'Request timed out. Please check your network connection.';
        } else if (err?.status === 404) {
          this.forgotError = 'No account found with that email address.';
        } else {
          const body = err?.error;
          this.forgotError = typeof body === 'string' ? body : (body?.message ?? 'Failed to send OTP. Please try again.');
        }
        this.cdr.detectChanges();
      }
    });
  }

  verifyOtp(): void {
    if (!this.forgotOtp) return;
    this.forgotLoading = true;
    this.forgotError = '';
    this.auth.verifyOtp(this.forgotEmail, this.forgotOtp).pipe(
      timeout(8000),
      finalize(() => { this.forgotLoading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: () => {
        this.forgotStep = 'reset';
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        if (err?.name === 'TimeoutError') {
          this.forgotError = 'Request timed out. Please try again.';
        } else {
          const body = err?.error;
          this.forgotError = typeof body === 'string' ? body : (body?.message ?? 'Invalid OTP. Please try again.');
        }
        this.cdr.detectChanges();
      }
    });
  }
  submitResetPassword(): void {
    if (this.newPassword !== this.confirmPassword) {
      this.forgotError = 'Passwords do not match.';
      this.cdr.detectChanges();
      return;
    }
    if (this.newPassword.length < 6) {
      this.forgotError = 'Password must be at least 6 characters.';
      this.cdr.detectChanges();
      return;
    }
    this.forgotLoading = true;
    this.forgotError = '';
    this.auth.verifyOtpAndReset(this.forgotEmail, this.forgotOtp, this.newPassword).pipe(
      timeout(10000),
      finalize(() => { this.forgotLoading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: () => {
        this.forgotSuccess = 'Password reset successful! You can now sign in.';
        this.cdr.detectChanges();
        setTimeout(() => { this.closeForgotModal(); this.cdr.detectChanges(); }, 2000);
      },
      error: (err: any) => {
        if (err?.name === 'TimeoutError') {
          this.forgotError = 'Request timed out. Please try again.';
        } else {
          const body = err?.error;
          this.forgotError = typeof body === 'string' ? body : (body?.message ?? 'Failed to reset password. Please try again.');
        }
        this.cdr.detectChanges();
      }
    });
  }
}
