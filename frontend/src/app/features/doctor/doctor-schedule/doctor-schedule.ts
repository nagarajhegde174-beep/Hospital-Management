import { ChangeDetectorRef, Component, OnDestroy,OnInit  } from '@angular/core';
import { Subscription } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ScheduleService } from '../../../core/services/schedule.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ErrorService } from '../../../core/error/error.service';
import { TimeSlot } from '../../../shared/models/time-slot.model';

@Component({
  selector: 'app-doctor-schedule',
  standalone: false,
  templateUrl: './doctor-schedule.html',
  styleUrl: './doctor-schedule.css'
})
export class DoctorSchedule implements OnInit, OnDestroy {

  // ── Create schedule ──
  form = { date: '', shiftStart: '09:00', shiftEnd: '17:00', breakStart: '' };
  loading = false;
  errorMessage = '';
  successMessage = '';

  // ── View slots ──
  viewDate = '';
  viewSlots: TimeSlot[] = [];
  viewLoading = false;
  viewError = '';
  availableDates: string[] = [];
datesLoading = false;

  private subs: Subscription[] = [];

  get minDate(): string {
    const d = new Date();
    d.setDate(d.getDate() + 3);
    return d.toISOString().split('T')[0];
  }

  constructor(
    private scheduleService: ScheduleService,
    private auth: AuthService,
    private errorService: ErrorService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
  this.loadAvailableDates();
}

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  submit(): void {
    const doctorId = this.auth.getServiceId();
    if (!doctorId || !this.form.date) return;

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.cdr.detectChanges();

    const payload: { doctorId: number; date: string; shiftStart: string; shiftEnd: string; breakStart?: string } = {
      doctorId,
      date: this.form.date,
      shiftStart: this.form.shiftStart,
      shiftEnd: this.form.shiftEnd
    };
    if (this.form.breakStart) payload.breakStart = this.form.breakStart;

    this.subs.push(this.scheduleService.setAvailability(payload).subscribe({
      next: (msg) => {
        this.loading = false;
        this.successMessage = msg || 'Schedule created successfully!';
        this.form = { date: '', shiftStart: '09:00', shiftEnd: '17:00', breakStart: '' };
        this.loadAvailableDates();
        this.cdr.detectChanges();
        // Auto-refresh view if viewing the same date
        if (this.viewDate === payload.date) this.loadSlots();
      },
     error: (err: HttpErrorResponse) => {
  this.loading = false;
  const body = err?.error;
  if (typeof body === 'string') {
    try {
      const parsed = JSON.parse(body);
      this.errorMessage = parsed?.message ?? body;
    } catch {
      this.errorMessage = body;
    }
  } else if (body?.message) {
    this.errorMessage = body.message;
  } else {
    this.errorMessage = this.errorService.handleHttpError(err);
  }
  this.cdr.detectChanges();
}
    }));
  }

  onViewDateChange(date: string): void {
    this.viewDate = date;
    if (date) this.loadSlots();
    else { this.viewSlots = []; this.viewError = ''; }
  }

  loadSlots(): void {
    const doctorId = this.auth.getServiceId();
    if (!doctorId || !this.viewDate) return;

    this.viewLoading = true;
    this.viewError = '';
    this.viewSlots = [];
    this.cdr.detectChanges();

    this.subs.push(this.scheduleService.getSlots(doctorId, this.viewDate).subscribe({
      next: (slots) => {
        this.viewSlots = slots;
        this.viewLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.viewError = this.errorService.handleHttpError(err);
        this.viewLoading = false;
        this.cdr.detectChanges();
      }
    }));
  }

  get bookedCount(): number { return this.viewSlots.filter(s => s.booked).length; }
  get freeCount(): number   { return this.viewSlots.filter(s => !s.booked).length; }

  formatSlot(iso: string): string {
    return new Date(iso).toLocaleTimeString('en-IN', {
      hour: '2-digit', minute: '2-digit', hour12: true
    });
  }
  loadAvailableDates(): void {
  const doctorId = this.auth.getServiceId();
  if (!doctorId) return;
  this.datesLoading = true;
  this.subs.push(this.scheduleService.getAvailableDates(doctorId).subscribe({
    next: (dates) => {
      this.availableDates = dates;
      this.datesLoading = false;
      this.cdr.detectChanges();
    },
    error: () => { this.datesLoading = false; this.cdr.detectChanges(); }
  }));
}

selectDate(date: string): void {
  this.viewDate = date;
  this.loadSlots();
}
}
