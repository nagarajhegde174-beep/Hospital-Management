import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap, timeout } from 'rxjs/operators';
import { Doctor } from '../../../shared/models/doctor.model';
import { TimeSlot } from '../../../shared/models/time-slot.model';
import { DoctorService } from '../../../core/services/doctor.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ErrorService } from '../../../core/error/error.service';

@Component({
  selector: 'app-book-appointment',
  standalone: false,
  templateUrl: './book-appointment.html',
  styleUrl: './book-appointment.css'
})
export class BookAppointment implements OnInit, OnDestroy {
  step = 1;

  specializations: string[] = [];
  selectedSpecialization = '';

  doctors: Doctor[] = [];
  selectedDoctor: Doctor | null = null;

  availableDates: string[] = [];
  selectedDate = '';
  slots: TimeSlot[] = [];
  selectedSlot: TimeSlot | null = null;

  loading = false;
  slotsLoading = false;
  errorMessage = '';
  confirmationCode = '';
  bookingSuccess = false;
  doctorRatings: { [id: number]: number } = {};

  private rescheduleCode = '';
  private timers: ReturnType<typeof setTimeout>[] = [];

  constructor(
    private doctorService: DoctorService,
    private appointmentService: AppointmentService,
    private auth: AuthService,
    private errorService: ErrorService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  get isReschedule(): boolean {
    return !!this.rescheduleCode;
  }

  // Helper: run a state update inside Angular's zone and trigger CD
  private update(fn: () => void): void {
    this.ngZone.run(() => { fn(); this.cdr.detectChanges(); });
  }

  ngOnDestroy(): void {
    this.timers.forEach(t => clearTimeout(t));
  }

  ngOnInit(): void {
    this.rescheduleCode = this.route.snapshot.queryParamMap.get('reschedule') ?? '';
    const doctorId = this.route.snapshot.queryParamMap.get('doctorId');

    if (doctorId) {
      // Reschedule flow: jump straight to step 3 — skip loading specializations
      this.loading = true;
      this.doctorService.getDoctorById(Number(doctorId)).pipe(
        catchError(() => of(null))
      ).subscribe(doc => {
        this.update(() => {
          this.loading = false;
          this.errorMessage = '';
          if (doc) {
            this.selectedSpecialization = doc.specialization;
            this.doctors = [doc];
            this.selectedDoctor = doc;
            this.step = 3;
          }
        });
        if (doc) this.loadAvailableDates();
      });
    } else {
      // Normal booking flow: start from step 1
      this.loadSpecializations();
    }
  }

  loadSpecializations(): void {
    this.update(() => { this.loading = true; this.errorMessage = ''; });

    this.doctorService.getAllDoctors().pipe(
      timeout(8000),
      catchError((err: any) => {
        const msg = err?.name === 'TimeoutError'
          ? 'Could not reach the server. Please ensure all backend services are running.'
          : (err?.status === 0
            ? 'Network error — cannot connect to the server (port 8090).'
            : this.errorService.handleHttpError(err as HttpErrorResponse));
        this.update(() => { this.loading = false; this.errorMessage = msg; });
        return of([] as Doctor[]);
      })
    ).subscribe((docs: Doctor[]) => {
      this.update(() => {
        this.loading = false;
        if (docs.length > 0) {
          const unique = new Set(docs.map(d => d.specialization));
          this.specializations = Array.from(unique).sort();
        }
      });
    });
  }

  selectSpecialization(spec: string): void {
    this.update(() => {
      this.selectedSpecialization = spec;
      this.selectedDoctor = null;
      this.step = 2;
      this.loading = true;
      this.errorMessage = '';
    });

    this.doctorService.getDoctorsBySpecialization(spec).pipe(
      timeout(8000),
      catchError((err: any) => {
        const msg = err?.name === 'TimeoutError'
          ? 'Could not reach the server. Please ensure all backend services are running.'
          : this.errorService.handleHttpError(err as HttpErrorResponse);
        this.update(() => { this.loading = false; this.errorMessage = msg; });
        return of([] as Doctor[]);
      }),
      switchMap((docs: Doctor[]) => {
        if (docs.length === 0) return of([] as Doctor[]);
        return forkJoin(
          docs.map(doc =>
            this.appointmentService.getAvailableDates(doc.id).pipe(
              catchError(() => of([] as string[])),
              map((dates: string[]) => ({ doc, hasSlots: dates.length > 0 }))
            )
          )
        ).pipe(
          map(results => results.filter(r => r.hasSlots).map(r => r.doc))
        );
      })
    ).subscribe((docs: Doctor[]) => {
      this.update(() => {
        this.loading = false;
        this.doctors = docs;
        this.loadDoctorRatings(docs);
        if (docs.length === 0 && !this.errorMessage) {
          this.errorMessage = 'No doctors with available slots found for this specialization.';
        }
      });
    });
  }

  selectDoctor(doctor: Doctor): void {
    this.update(() => {
      this.selectedDoctor = doctor;
      this.step = 3;
      this.selectedDate = '';
      this.slots = [];
      this.selectedSlot = null;
      this.errorMessage = '';   // clear any previous step's error
    });
    this.loadAvailableDates();
  }

  private loadAvailableDates(): void {
    if (!this.selectedDoctor) return;
    this.update(() => { this.loading = true; });

    this.appointmentService.getAvailableDates(this.selectedDoctor!.id).pipe(
      timeout(8000),
      catchError((err: any) => {
        const msg = err?.name === 'TimeoutError'
          ? 'Could not reach the server. Please ensure all backend services are running.'
          : this.errorService.handleHttpError(err as HttpErrorResponse);
        this.update(() => { this.loading = false; this.errorMessage = msg; });
        return of([] as string[]);
      })
    ).subscribe((dates: string[]) => {
      this.update(() => {
        this.loading = false;
        this.errorMessage = '';   // clear any stale error from previous steps
        this.availableDates = dates;
      });
    });
  }

  onDateChange(date: string): void {
    this.update(() => {
      this.selectedDate = date;
      this.selectedSlot = null;
      this.slots = [];
    });
    if (!date || !this.selectedDoctor) return;

    this.update(() => { this.slotsLoading = true; this.errorMessage = ''; });

    this.appointmentService.getAvailableSlots(this.selectedDoctor!.id, date).pipe(
      timeout(8000),
      catchError((err: any) => {
        const msg = err?.name === 'TimeoutError'
          ? 'Could not reach the server. Please ensure all backend services are running.'
          : this.errorService.handleHttpError(err as HttpErrorResponse);
        this.update(() => { this.slotsLoading = false; this.errorMessage = msg; });
        return of([] as TimeSlot[]);
      })
    ).subscribe((slots: TimeSlot[]) => {
      this.update(() => {
        this.slotsLoading = false;
        this.slots = slots;
      });
    });
  }

  selectSlot(slot: TimeSlot): void {
    this.update(() => { this.selectedSlot = slot; });
  }

  confirmBooking(): void {
    if (!this.selectedDoctor || !this.selectedSlot) return;
    this.update(() => { this.loading = true; this.errorMessage = ''; });

    const obs$ = this.rescheduleCode
      ? this.appointmentService.rescheduleAppointment(
          this.rescheduleCode, this.selectedSlot.startTime)
      : (() => {
          const patientId = this.auth.getServiceId();
          if (!patientId) {
            this.update(() => { this.loading = false; this.errorMessage = 'Session expired. Please log in again.'; });
            return null as any;
          }
          return this.appointmentService.bookAppointment(
            patientId, this.selectedDoctor!.id, this.selectedSlot!.startTime);
        })();

    if (!obs$) return;

    obs$.pipe(
      timeout(8000),
      catchError((err: any) => {
        const msg = err?.name === 'TimeoutError'
          ? 'Could not reach the booking server. Please try again.'
          : this.errorService.handleHttpError(err as HttpErrorResponse);
        this.update(() => { this.loading = false; this.errorMessage = msg; });
        return of(null);
      })
    ).subscribe((appt: any) => {
      this.update(() => {
        this.loading = false;
        if (appt) {
          this.bookingSuccess = true;
          this.confirmationCode = appt.confirmationCode;
          this.timers.push(setTimeout(() => this.router.navigate(['/patient/appointments']), 2500));
        }
      });
    });
  }

  goBack(): void {
    this.update(() => { this.step--; this.errorMessage = ''; });
  }

  formatSlotTime(isoTime: string): string {
    const date = new Date(isoTime);
    return date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true });
  }
  private loadDoctorRatings(doctors: Doctor[]): void {
  doctors.forEach(doc => {
    this.doctorService.getAverageRating(doc.id).subscribe({
      next: (avg) => {
        this.update(() => { this.doctorRatings[doc.id] = avg; });
      },
      error: () => { this.doctorRatings[doc.id] = 0; }
    });
  });
}
}
