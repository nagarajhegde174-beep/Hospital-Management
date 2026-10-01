import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, timeout } from 'rxjs/operators';
import { Appointment } from '../../../shared/models/appointment.model';
import { AppointmentService } from '../../../core/services/appointment.service';
import { DoctorService } from '../../../core/services/doctor.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ErrorService } from '../../../core/error/error.service';
import { Subscription } from 'rxjs';

type Tab = 'upcoming' | 'past' | 'cancelled';

@Component({
  selector: 'app-my-appointments',
  standalone: false,
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.css'
})
export class MyAppointments implements OnInit, OnDestroy {
  activeTab: Tab = 'upcoming';
  all: Appointment[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';

  searchTerm = '';
  pageSize = 5;
  currentPage = 0;

  doctorNames: { [id: number]: string } = {};
  actionLoading: { [code: string]: boolean } = {};
  ratedAppointments: Set<number> = new Set();
showRatingModal = false;
ratingAppointment: Appointment | null = null;
ratingValue = 0;
ratingLoading = false;
ratingError = '';
ratingSuccess = '';
  private timers: ReturnType<typeof setTimeout>[] = [];

  constructor(
    private appointmentService: AppointmentService,
    private doctorService: DoctorService,
    private auth: AuthService,
    private errorService: ErrorService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.timers.forEach(t => clearTimeout(t));
  }

  private load(): void {
    const patientId = this.auth.getServiceId();
    if (!patientId) return;

    this.loading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.appointmentService.getPatientAppointments(patientId).pipe(
      timeout(8000),
      finalize(() => { this.loading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: (appts) => {
        this.all = appts;
        this.resolveDoctorNames(appts);
        this.loadRatedAppointments(appts); 
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

  get upcoming(): Appointment[] {
    return this.all.filter(a =>
      (a.status === 'CONFIRMED' || a.status === 'RESCHEDULED') &&
      new Date(a.appointmentDate) >= new Date()
    );
  }

  get past(): Appointment[] {
    return this.all.filter(a =>
      a.status === 'COMPLETED' ||
      ((a.status === 'CONFIRMED' || a.status === 'RESCHEDULED') &&
        new Date(a.appointmentDate) < new Date())
    );
  }

  get cancelled(): Appointment[] {
    return this.all.filter(a => a.status === 'CANCELLED');
  }

  activeList(): Appointment[] {
    if (this.activeTab === 'upcoming') return this.upcoming;
    if (this.activeTab === 'past') return this.past;
    return this.cancelled;
  }

  get filtered(): Appointment[] {
    const term = this.searchTerm.toLowerCase().trim();
    if (!term) return this.activeList();
    return this.activeList().filter(a =>
      this.formatDate(a.appointmentDate).toLowerCase().includes(term) ||
      a.confirmationCode.toLowerCase().includes(term) ||
      (this.doctorNames[a.doctorId] ?? '').toLowerCase().includes(term)
    );
  }

  get paged(): Appointment[] {
    const start = this.currentPage * this.pageSize;
    return this.filtered.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.ceil(this.filtered.length / this.pageSize);
  }

  prevPage(): void { if (this.currentPage > 0) this.currentPage--; }
  nextPage(): void { if (this.currentPage < this.totalPages - 1) this.currentPage++; }

  setTab(tab: Tab): void {
    this.activeTab = tab;
    this.searchTerm = '';
    this.currentPage = 0;
    this.errorMessage = '';
    this.successMessage = '';
  }

  onSearchChange(): void { this.currentPage = 0; }

  confirmCancel(appt: Appointment): void {
    if (!confirm('Cancel this appointment? This cannot be undone.')) return;
    this.cancel(appt);
  }

  cancel(appt: Appointment): void {
    this.actionLoading[appt.confirmationCode] = true;
    this.errorMessage = '';
    this.appointmentService.cancelAppointment(appt.confirmationCode).subscribe({
      next: () => {
        this.actionLoading[appt.confirmationCode] = false;
        appt.status = 'CANCELLED';
        this.successMessage = 'Appointment cancelled successfully.';
        this.cdr.detectChanges();
        this.timers.push(setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000));
      },
      error: (err: HttpErrorResponse) => {
        this.actionLoading[appt.confirmationCode] = false;
        this.errorMessage = this.errorService.handleHttpError(err);
        this.cdr.detectChanges();
      }
    });
  }

  reschedule(appt: Appointment): void {
    this.router.navigate(['/patient/book-appointment'], {
      queryParams: { doctorId: appt.doctorId, reschedule: appt.confirmationCode }
    });
  }

  private resolveDoctorNames(appts: Appointment[]): void {
    const ids = [...new Set(appts.map(a => a.doctorId))].filter(id => !this.doctorNames[id]);
    if (!ids.length) return;
    const calls = Object.fromEntries(
      ids.map(id => [id, this.doctorService.getDoctorById(id).pipe(catchError(() => of(null)))])
    );
    forkJoin(calls).subscribe({
      next: (results) => {
        Object.entries(results).forEach(([id, doc]: [string, any]) => {
          this.doctorNames[+id] = doc?.name ?? 'Unknown Doctor';
        });
        this.cdr.detectChanges();
      }
    });
  }

  trackByAppt(i: number, a: Appointment): string { return a.confirmationCode; }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('en-IN', {
      hour: '2-digit', minute: '2-digit', hour12: true
    });
  }
  openRatingModal(appt: Appointment): void {
  this.ratingAppointment = appt;
  this.ratingValue = 0;
  this.ratingError = '';
  this.ratingSuccess = '';
  this.showRatingModal = true;
  this.cdr.detectChanges()
}

closeRatingModal(): void {
  this.showRatingModal = false;
  this.ratingAppointment = null;
}

setRating(value: number): void {
  this.ratingValue = value;
}

submitRating(): void {
  if (this.ratingValue === 0) {
    this.ratingError = 'Please select a star rating.';
    return;
  }
  if (!this.ratingAppointment) return;

  this.ratingLoading = true;
  this.ratingError = '';

  const patientId = this.auth.getServiceId();
  const payload = {
    doctorId: this.ratingAppointment.doctorId,
    patientId: patientId!,
    appointmentId: this.ratingAppointment.appointmentId,
    rating: this.ratingValue
  };

  this.doctorService.submitRating(payload).subscribe({
    next: () => {
      this.ratedAppointments.add(this.ratingAppointment!.appointmentId);
      this.ratingSuccess = 'Thank you for your feedback!';
      this.ratingLoading = false;
      this.cdr.detectChanges();
      this.timers.push(setTimeout(() => { this.closeRatingModal(); this.cdr.detectChanges(); }, 2000));
    },
   error: (err) => {
  this.ratingLoading = false;
  const body = err?.error;
  this.ratingError = typeof body === 'string' ? body : (body?.message ?? 'Failed to submit rating.');
  this.cdr.detectChanges();
}
  });
}
private loadRatedAppointments(appts: Appointment[]): void {
  const completed = appts.filter(a => a.status === 'COMPLETED');
  completed.forEach(a => {
    this.doctorService.hasRated(a.appointmentId).subscribe({
      next: (rated) => {
        if (rated) {
          this.ratedAppointments.add(a.appointmentId);
          this.cdr.detectChanges();
        }
      },
      error: () => {}
    });
  });
}
}
