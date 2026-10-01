import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { timeout } from 'rxjs/operators';
import { Patient } from '../../../shared/models/patient.model';
import { MedicalHistory } from '../../../shared/models/medical-history.model';
import { PatientService } from '../../../core/services/patient.service';
import { MedicalHistoryService } from '../../../core/services/medical-history.service';

@Component({
  selector: 'app-admin-patients',
  standalone: false,
  templateUrl: './admin-patients.html',
  styleUrl: './admin-patients.css'
})
export class AdminPatients implements OnInit, OnDestroy {
  patients: Patient[] = [];
  loading = true;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';
  pageSize = 8;
  currentPage = 0;
  actionLoading: { [id: number]: boolean } = {};

  viewingHistoryFor: number | null = null;
  patientHistory: { [patientId: number]: MedicalHistory[] } = {};
  historyLoading: { [patientId: number]: boolean } = {};
  recordActionLoading: { [recordId: number]: boolean } = {};

  private subs: Subscription[] = [];
  private timers: ReturnType<typeof setTimeout>[] = [];

  constructor(
    private patientService: PatientService,
    private historyService: MedicalHistoryService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.timers.forEach(t => clearTimeout(t));
  }

  ngOnInit(): void {
    this.subs.push(this.patientService.getAllPatients().pipe(timeout(10000)).subscribe({
      next: (pts) => { this.patients = pts; this.loading = false; this.cdr.detectChanges(); },
      error: (err: any) => {
        this.errorMessage = err?.name === 'TimeoutError'
          ? 'Request timed out. Please check backend services are running.'
          : 'Failed to load patients.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    }));
  }

  trackByPatient(i: number, p: Patient): number { return p.patientId; }
  trackByRecord(i: number, r: MedicalHistory): number { return r.recordId; }

  get filtered(): Patient[] {
    const term = this.searchTerm.toLowerCase().trim();
    if (!term) return this.patients;
    return this.patients.filter(p =>
      p.name.toLowerCase().includes(term) ||
      p.gender.toLowerCase().includes(term) ||
      p.contactDetails.includes(term)
    );
  }

  get paged(): Patient[] {
    const start = this.currentPage * this.pageSize;
    return this.filtered.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.ceil(this.filtered.length / this.pageSize);
  }

  prevPage(): void { if (this.currentPage > 0) this.currentPage--; }
  nextPage(): void { if (this.currentPage < this.totalPages - 1) this.currentPage++; }
  onSearchChange(): void { this.currentPage = 0; }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  deletePatient(patient: Patient): void {
    if (!confirm(`Delete patient "${patient.name}"? This cannot be undone.`)) return;
    this.actionLoading[patient.patientId] = true;
    this.errorMessage = '';
    this.subs.push(this.patientService.deletePatient(patient.patientId).subscribe({
      next: () => {
        this.patients = this.patients.filter(p => p.patientId !== patient.patientId);
        delete this.actionLoading[patient.patientId];
        if (this.viewingHistoryFor === patient.patientId) this.viewingHistoryFor = null;
        this.successMessage = `Patient "${patient.name}" has been deleted.`;
        this.cdr.detectChanges();
        this.timers.push(setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000));
      },
      error: () => {
        this.actionLoading[patient.patientId] = false;
        this.errorMessage = 'Failed to delete patient. Please try again.';
        this.cdr.detectChanges();
      }
    }));
  }

  toggleHistory(patient: Patient): void {
    if (this.viewingHistoryFor === patient.patientId) {
      this.viewingHistoryFor = null;
      return;
    }
    this.viewingHistoryFor = patient.patientId;
    if (this.patientHistory[patient.patientId] !== undefined) return;

    this.historyLoading[patient.patientId] = true;
    this.subs.push(this.historyService.getPatientHistory(patient.patientId).subscribe({
      next: (records) => {
        this.patientHistory[patient.patientId] = [...records].sort(
          (a, b) => new Date(b.diagnosedAt).getTime() - new Date(a.diagnosedAt).getTime()
        );
        this.historyLoading[patient.patientId] = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.patientHistory[patient.patientId] = [];
        this.historyLoading[patient.patientId] = false;
        this.cdr.detectChanges();
      }
    }));
  }

  deleteRecord(patientId: number, record: MedicalHistory): void {
    if (!confirm(`Delete record "${record.diagnosis}" (${record.diagnosedAt})? This cannot be undone.`)) return;
    this.recordActionLoading[record.recordId] = true;
    this.errorMessage = '';
    this.subs.push(this.historyService.deleteRecord(record.recordId).subscribe({
      next: () => {
        this.patientHistory[patientId] = this.patientHistory[patientId].filter(r => r.recordId !== record.recordId);
        delete this.recordActionLoading[record.recordId];
        this.successMessage = 'Medical record deleted.';
        this.cdr.detectChanges();
        this.timers.push(setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000));
      },
      error: () => {
        this.recordActionLoading[record.recordId] = false;
        this.errorMessage = 'Failed to delete record. Please try again.';
        this.cdr.detectChanges();
      }
    }));
  }
}
