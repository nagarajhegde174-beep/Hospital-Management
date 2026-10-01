import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, timeout } from 'rxjs/operators';
import { Appointment } from '../../../shared/models/appointment.model';
import { MedicalHistory } from '../../../shared/models/medical-history.model';
import { Patient } from '../../../shared/models/patient.model';
import { AppointmentService } from '../../../core/services/appointment.service';
import { MedicalHistoryService } from '../../../core/services/medical-history.service';
import { PatientService } from '../../../core/services/patient.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ErrorService } from '../../../core/error/error.service';
import { DoctorService } from '../../../core/services/doctor.service';

type Tab = 'upcoming' | 'past';

@Component({
  selector: 'app-doctor-appointments',
  standalone: false,
  templateUrl: './doctor-appointments.html',
  styleUrl: './doctor-appointments.css'
})
export class DoctorAppointments implements OnInit, OnDestroy {
  activeTab: Tab = 'upcoming';
  all: Appointment[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  actionLoading: { [code: string]: boolean } = {};
  myRating: number | null = null;
  patientMap: { [id: number]: Patient } = {};

  // Search & pagination
  searchTerm = '';
  pageSize = 5;
  currentPage = 0;

  private timers: ReturnType<typeof setTimeout>[] = [];
  prescribingFor: string | null = null;
  viewingHistoryFor: string | null = null;
  editingRecordId: number | null = null;
  prescriptionForm = { diagnosis: '', diagnosedAt: '', medicInput: '', meds: [] as string[] };
  prescriptionLoading: { [code: string]: boolean } = {};

  // Stores all past records per appointment confirmation code for the history panel
  patientHistory: { [code: string]: MedicalHistory[] } = {};
  appointmentRatings: { [appointmentId: number]: number } = {};
  constructor(
    private appointmentService: AppointmentService,
    private historyService: MedicalHistoryService,
    private patientService: PatientService,
    private auth: AuthService,
    private errorService: ErrorService,
    private doctorService: DoctorService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.load();
    this.loadMyRating();

  }

  ngOnDestroy(): void {
    this.timers.forEach(t => clearTimeout(t));
  }

  private load(): void {
    const doctorId = this.auth.getServiceId();
    if (!doctorId) return;

    this.loading = true;
    this.cdr.detectChanges();

    this.appointmentService.getDoctorAppointments(doctorId).pipe(
      timeout(8000),
      finalize(() => { this.loading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: (appts) => {
        this.all = appts;
        this.fetchPatientDetails(appts);
        this.loadPrescriptionStatus(appts);
        this.loadAppointmentRatings(appts); 
      },
      error: (err: any) => {
        this.errorMessage = err?.name === 'TimeoutError'
          ? 'Request timed out. Please check backend services are running.'
          : this.errorService.handleHttpError(err as HttpErrorResponse);
        this.cdr.detectChanges();
      }
    });
  }

  private fetchPatientDetails(appts: Appointment[]): void {
    const ids = [...new Set(appts.map(a => a.patientId))].filter(id => !this.patientMap[id]);
    if (!ids.length) return;
    const calls = Object.fromEntries(
      ids.map(id => [id, this.patientService.getPatientById(id).pipe(catchError(() => of(null)))])
    );
    forkJoin(calls).subscribe({
      next: (results) => {
        Object.entries(results).forEach(([id, p]: [string, any]) => {
          if (p) this.patientMap[+id] = p;
        });
        this.cdr.detectChanges();
      }
    });
  }

  getPatientName(id: number): string {
    return this.patientMap[id]?.name ?? `Patient #${id}`;
  }

  getPatientContact(id: number): string {
    return this.patientMap[id]?.contactDetails ?? '';
  }

  get upcoming(): Appointment[] {
    return this.all.filter(a =>
      (a.status === 'CONFIRMED' || a.status === 'RESCHEDULED') &&
      new Date(a.appointmentDate) >= new Date()
    );
  }

  get past(): Appointment[] {
    return this.all.filter(a =>
      a.status === 'COMPLETED' || a.status === 'CANCELLED' ||
      ((a.status === 'CONFIRMED' || a.status === 'RESCHEDULED') &&
        new Date(a.appointmentDate) < new Date())
    );
  }

  activeList(): Appointment[] {
    return this.activeTab === 'upcoming' ? this.upcoming : this.past;
  }

  get filtered(): Appointment[] {
    const term = this.searchTerm.toLowerCase().trim();
    if (!term) return this.activeList();
    return this.activeList().filter(a =>
      this.getPatientName(a.patientId).toLowerCase().includes(term) ||
      this.formatDate(a.appointmentDate).toLowerCase().includes(term) ||
      a.confirmationCode.toLowerCase().includes(term)
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
  onSearchChange(): void { this.currentPage = 0; }

  setTab(tab: Tab): void {
    this.activeTab = tab;
    this.searchTerm = '';
    this.currentPage = 0;
    this.errorMessage = '';
    this.successMessage = '';
  }

  confirmComplete(appt: Appointment): void {
    if (!confirm(`Mark appointment with ${this.getPatientName(appt.patientId)} as completed?`)) return;
    this.complete(appt);
  }

  confirmCancel(appt: Appointment): void {
    if (!confirm(`Cancel appointment with ${this.getPatientName(appt.patientId)}? This cannot be undone.`)) return;
    this.cancel(appt);
  }

  complete(appt: Appointment): void {
    this.actionLoading[appt.confirmationCode] = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.appointmentService.completeAppointment(appt.confirmationCode).subscribe({
      next: () => {
        this.actionLoading[appt.confirmationCode] = false;
        appt.status = 'COMPLETED';
        this.successMessage = `Appointment with ${this.getPatientName(appt.patientId)} marked as completed.`;
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

  cancel(appt: Appointment): void {
    this.actionLoading[appt.confirmationCode] = true;
    this.errorMessage = '';

    this.appointmentService.cancelAppointment(appt.confirmationCode).subscribe({
      next: () => {
        this.actionLoading[appt.confirmationCode] = false;
        appt.status = 'CANCELLED';
        this.successMessage = `Appointment with ${this.getPatientName(appt.patientId)} has been cancelled.`;
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

  toggleHistory(appt: Appointment): void {
    if (this.viewingHistoryFor === appt.confirmationCode) {
      this.viewingHistoryFor = null;
      return;
    }
    this.viewingHistoryFor = appt.confirmationCode;
    this.prescribingFor = null;

    if (this.patientHistory[appt.confirmationCode] !== undefined) return;

    this.appointmentService.getPatientHistoryForAppointment(appt.confirmationCode).subscribe({
      next: (records) => {
        this.patientHistory[appt.confirmationCode] = [...records].sort(
          (a, b) => new Date(b.diagnosedAt).getTime() - new Date(a.diagnosedAt).getTime()
        );
        this.cdr.detectChanges();
      },
      error: () => {
        this.patientHistory[appt.confirmationCode] = [];
        this.cdr.detectChanges();
      }
    });
  }

  startPrescription(appt: Appointment): void {
    this.prescribingFor = appt.confirmationCode;
    this.viewingHistoryFor = null;
    this.editingRecordId = null;
    this.prescriptionForm = {
      diagnosis: '',
      diagnosedAt: new Date().toISOString().split('T')[0],
      medicInput: '',
      meds: []
    };
    this.errorMessage = '';

    const today = new Date().toISOString().split('T')[0];
    this.appointmentService.getPatientHistoryForAppointment(appt.confirmationCode).subscribe({
      next: (records) => {
        // Store ALL records for the history panel (sorted newest first)
        this.patientHistory[appt.confirmationCode] = [...records].sort(
          (a, b) => new Date(b.diagnosedAt).getTime() - new Date(a.diagnosedAt).getTime()
        );

        // Pre-fill the form if a record already exists for today's date
        const existing = records.find(r => r.diagnosedAt === today);
        if (existing) {
          this.editingRecordId = existing.recordId;
          this.prescriptionForm.diagnosis = existing.diagnosis;
          this.prescriptionForm.meds = [...(existing.prescribedMeds || [])];
        }
        this.cdr.detectChanges();
      },
      error: () => {
        // History failed to load — open empty form anyway
        this.patientHistory[appt.confirmationCode] = [];
        this.cdr.detectChanges();
      }
    });
  }

  cancelPrescription(): void {
    this.prescribingFor = null;
    this.editingRecordId = null;
  }

  addMed(): void {
    const med = this.prescriptionForm.medicInput.trim();
    if (med && !this.prescriptionForm.meds.includes(med)) {
      this.prescriptionForm.meds.push(med);
    }
    this.prescriptionForm.medicInput = '';
  }

  removeMed(index: number): void {
    this.prescriptionForm.meds.splice(index, 1);
  }

  // Returns past records excluding the one currently being edited (to avoid duplication)
  getPastHistory(code: string): MedicalHistory[] {
    const all = this.patientHistory[code] ?? [];
    return all.filter(r => r.recordId !== this.editingRecordId);
  }

  submitPrescription(appt: Appointment): void {
    if (!this.prescriptionForm.diagnosis.trim()) return;
    if (this.prescriptionForm.meds.length === 0) {
      this.errorMessage = 'Please add at least one medication before saving.';
      this.cdr.detectChanges();
      return;
    }

    this.prescriptionLoading[appt.confirmationCode] = true;
    this.errorMessage = '';

    const obs = this.editingRecordId
      ? this.historyService.updateRecord(this.editingRecordId, {
        diagnosis: this.prescriptionForm.diagnosis.trim(),
        prescribedMeds: this.prescriptionForm.meds
      })
      : this.historyService.addRecord({
        patientId: appt.patientId,
        doctorId: appt.doctorId,
        diagnosis: this.prescriptionForm.diagnosis.trim(),
        diagnosedAt: this.prescriptionForm.diagnosedAt,
        prescribedMeds: this.prescriptionForm.meds
      });

    obs.subscribe({
      next: () => {
        const wasEditing = this.editingRecordId !== null;
        const code = appt.confirmationCode;
        const patientName = this.getPatientName(appt.patientId);
        const closeForm = () => {
          this.prescriptionLoading[code] = false;
          this.prescribingFor = null;
          this.editingRecordId = null;
          this.successMessage = `Prescription ${wasEditing ? 'updated' : 'saved'} for ${patientName}.`;
          this.cdr.detectChanges();
          this.timers.push(setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3500));
        };
        // Refresh history first so hasPrescription() is up-to-date before form closes
        this.appointmentService.getPatientHistoryForAppointment(code).subscribe({
          next: (records) => {
            this.patientHistory[code] = [...records].sort(
              (a, b) => new Date(b.diagnosedAt).getTime() - new Date(a.diagnosedAt).getTime()
            );
            closeForm();
          },
          error: () => closeForm()
        });
      },
      error: (err: HttpErrorResponse) => {
        this.prescriptionLoading[appt.confirmationCode] = false;
        this.errorMessage = this.errorService.handleHttpError(err);
        this.cdr.detectChanges();
      }
    });
  }

  private loadPrescriptionStatus(appts: Appointment[]): void {
    const upcoming = appts.filter(a =>
      (a.status === 'CONFIRMED' || a.status === 'RESCHEDULED') &&
      new Date(a.appointmentDate) >= new Date()
    );
    upcoming.forEach(a => {
      if (this.patientHistory[a.confirmationCode] !== undefined) return;
      this.appointmentService.getPatientHistoryForAppointment(a.confirmationCode).subscribe({
        next: (records) => {
          this.patientHistory[a.confirmationCode] = records;
          this.cdr.detectChanges();
        },
        error: () => {
          this.patientHistory[a.confirmationCode] = [];
          this.cdr.detectChanges();
        }
      });
    });
  }

  hasPrescription(code: string): boolean {
  const history = this.patientHistory[code];
  if (history === undefined) return true; // still loading — don't block the button
  const today = new Date().toISOString().split('T')[0];
  return history.some(r => r.diagnosedAt === today);
}

  trackByAppt(i: number, a: Appointment): string { return a.confirmationCode; }
  trackByRecord(i: number, r: MedicalHistory): number { return r.recordId; }
  trackByIdx(i: number): number { return i; }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-IN', {
      weekday: 'short', day: '2-digit', month: 'short', year: 'numeric'
    });
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('en-IN', {
      hour: '2-digit', minute: '2-digit', hour12: true
    });
  }
  private loadMyRating(): void {
    const doctorId = this.auth.getServiceId();
    if (!doctorId) return;
    this.doctorService.getAverageRating(doctorId).subscribe({
      next: (avg) => { this.myRating = avg; this.cdr.detectChanges(); },
      error: () => { }
    });
  }
  private loadAppointmentRatings(appts: Appointment[]): void {
    const completed = appts.filter(a => a.status === 'COMPLETED');
    completed.forEach(a => {
      this.doctorService.getRatingByAppointment(a.appointmentId).subscribe({
        next: (rating) => {
          this.appointmentRatings[a.appointmentId] = rating;
          this.cdr.detectChanges();
        },
        error: () => { this.appointmentRatings[a.appointmentId] = 0; }
      });
    });
  }
}
