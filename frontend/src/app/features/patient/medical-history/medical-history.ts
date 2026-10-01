import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, timeout } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { MedicalHistory } from '../../../shared/models/medical-history.model';
import { MedicalHistoryService } from '../../../core/services/medical-history.service';
import { DoctorService } from '../../../core/services/doctor.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ErrorService } from '../../../core/error/error.service';

@Component({
  selector: 'app-medical-history',
  standalone: false,
  templateUrl: './medical-history.html',
  styleUrl: './medical-history.css'
})
export class MedicalHistoryPage implements OnInit, OnDestroy {
  records: MedicalHistory[] = [];
  doctorNames: { [id: number]: string } = {};
  loading = false;
  errorMessage = '';
  successMessage = '';
  private timers: ReturnType<typeof setTimeout>[] = [];

  // Search & sort
  searchTerm = '';
  sortOrder: 'desc' | 'asc' = 'desc';

  // Pagination
  pageSize = 5;
  currentPage = 0;

  constructor(
    private historyService: MedicalHistoryService,
    private doctorService: DoctorService,
    private auth: AuthService,
    private errorService: ErrorService,
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

    this.historyService.getPatientHistory(patientId).pipe(
      timeout(8000),
      finalize(() => { this.loading = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: (records) => {
        this.records = records;
        this.resolveDoctorNames(records);
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

  get filtered(): MedicalHistory[] {
    let result = [...this.records];
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(r =>
        r.diagnosis.toLowerCase().includes(term) ||
        (r.prescribedMeds ?? []).some(m => m.toLowerCase().includes(term))
      );
    }
    result.sort((a, b) => {
      const diff = new Date(a.diagnosedAt).getTime() - new Date(b.diagnosedAt).getTime();
      return this.sortOrder === 'desc' ? -diff : diff;
    });
    return result;
  }

  get paged(): MedicalHistory[] {
    const start = this.currentPage * this.pageSize;
    return this.filtered.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.ceil(this.filtered.length / this.pageSize);
  }

  prevPage(): void { if (this.currentPage > 0) this.currentPage--; }
  nextPage(): void { if (this.currentPage < this.totalPages - 1) this.currentPage++; }

  onSearchChange(): void { this.currentPage = 0; }

  toggleSort(): void {
    this.sortOrder = this.sortOrder === 'desc' ? 'asc' : 'desc';
    this.currentPage = 0;
  }

  private resolveDoctorNames(records: MedicalHistory[]): void {
    const ids = [...new Set(records.map(r => r.doctorId).filter((id): id is number => !!id))]
      .filter(id => !this.doctorNames[id]);
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

  trackByRecord(i: number, r: MedicalHistory): number { return r.recordId; }
  trackByIdx(i: number): number { return i; }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }
}
