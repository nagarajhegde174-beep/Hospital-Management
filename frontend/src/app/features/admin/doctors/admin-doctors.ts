import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { timeout } from 'rxjs/operators';
import { Doctor } from '../../../shared/models/doctor.model';
import { DoctorService } from '../../../core/services/doctor.service';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-admin-doctors',
  standalone: false,
  templateUrl: './admin-doctors.html',
  styleUrl: './admin-doctors.css'
})
export class AdminDoctors implements OnInit, OnDestroy {
  doctors: Doctor[] = [];
  loading = true;
  errorMessage = '';
  successMessage = '';
  searchTerm = '';
  pageSize = 8;
  currentPage = 0;
  actionLoading: { [id: number]: boolean } = {};
  doctorRatings: { [id: number]: number | null } = {};
  pendingDoctors: any[] = [];
  pendingLoading = false;
  pendingActionLoading: { [id: number]: boolean } = {};
  activeTab: 'registered' | 'pending' = 'registered';

  private subs: Subscription[] = [];
  private timers: ReturnType<typeof setTimeout>[] = [];

  constructor(
    private doctorService: DoctorService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.timers.forEach(t => clearTimeout(t));
  }

  ngOnInit(): void {
    this.loading = true;
    this.subs.push(this.doctorService.getAllDoctors().pipe(timeout(10000)).subscribe({
      next: (docs) => {
        this.doctors = docs;
        this.loading = false;
        this.loadRatings(docs);
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        this.errorMessage = err?.name === 'TimeoutError'
          ? 'Request timed out. Please check backend services are running.'
          : 'Failed to load doctors.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    }));
  }
  trackByDoc(i: number, d: Doctor): number { return d.id; }

  get filtered(): Doctor[] {
    const term = this.searchTerm.toLowerCase().trim();
    if (!term) return this.doctors;
    return this.doctors.filter(d =>
      d.name.toLowerCase().includes(term) ||
      d.specialization.toLowerCase().includes(term) ||
      d.designation.toLowerCase().includes(term)
    );
  }

  get paged(): Doctor[] {
    const start = this.currentPage * this.pageSize;
    return this.filtered.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.ceil(this.filtered.length / this.pageSize);
  }

  prevPage(): void { if (this.currentPage > 0) this.currentPage--; }
  nextPage(): void { if (this.currentPage < this.totalPages - 1) this.currentPage++; }
  onSearchChange(): void { this.currentPage = 0; }

  deleteDoctor(doctor: Doctor): void {
    if (!confirm(`Delete doctor "${doctor.name}"? This cannot be undone.`)) return;
    this.actionLoading[doctor.id] = true;
    this.errorMessage = '';
    this.subs.push(this.doctorService.deleteDoctor(doctor.id).subscribe({
      next: () => {
        this.doctors = this.doctors.filter(d => d.id !== doctor.id);
        delete this.actionLoading[doctor.id];
        this.successMessage = `Doctor "${doctor.name}" has been deleted.`;
        this.cdr.detectChanges();
        this.timers.push(setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000));
      },
      error: () => {
        this.actionLoading[doctor.id] = false;
        this.errorMessage = 'Failed to delete doctor. Please try again.';
        this.cdr.detectChanges();
      }
    }));
  }
  switchTab(tab: 'registered' | 'pending'): void {
    this.activeTab = tab;
    this.cdr.detectChanges();
    if (tab === 'pending' && this.pendingDoctors.length === 0) {
      this.loadPendingDoctors();
    }
  }

  loadPendingDoctors(): void {
    this.pendingLoading = true;
    this.subs.push(this.authService.getPendingDoctors().subscribe({
      next: (docs) => { this.pendingDoctors = docs; this.pendingLoading = false; this.cdr.detectChanges(); },
      error: () => { this.pendingLoading = false; this.cdr.detectChanges(); }
    }));
  }

  approveDoctor(doctor: any): void {
    this.pendingActionLoading[doctor.userId] = true;
    this.subs.push(this.authService.approveDoctor(doctor.userId).subscribe({
      next: () => {
        this.pendingDoctors = this.pendingDoctors.filter(d => d.userId !== doctor.userId);
        delete this.pendingActionLoading[doctor.userId];
        this.successMessage = `Doctor "${doctor.email}" approved.`;
        this.loadDoctors();
        this.cdr.detectChanges();
        this.timers.push(setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000));
      },
      error: () => {
        this.pendingActionLoading[doctor.userId] = false;
        this.errorMessage = 'Failed to approve doctor.';
        this.cdr.detectChanges();
      }
    }));
  }
  private loadDoctors(): void {
    this.subs.push(this.doctorService.getAllDoctors().pipe(timeout(10000)).subscribe({
      next: (docs) => {
        this.doctors = docs;
        this.loadRatings(docs);
        this.cdr.detectChanges();
      },
      error: () => { this.cdr.detectChanges(); }
    }));
  }

  rejectDoctor(doctor: any): void {
    if (!confirm(`Reject and remove doctor "${doctor.email}"?`)) return;
    this.pendingActionLoading[doctor.userId] = true;
    this.subs.push(this.authService.rejectDoctor(doctor.userId).subscribe({
      next: () => {
        this.pendingDoctors = this.pendingDoctors.filter(d => d.userId !== doctor.userId);
        this.doctors = this.doctors.filter(d => d.email !== doctor.email);
        delete this.pendingActionLoading[doctor.userId];
        this.successMessage = `Doctor "${doctor.email}" rejected.`;
        this.cdr.detectChanges();
        this.timers.push(setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000));
      },
      error: () => {
        this.pendingActionLoading[doctor.userId] = false;
        this.errorMessage = 'Failed to reject doctor.';
        this.cdr.detectChanges();
      }
    }));
  }
  private loadRatings(doctors: Doctor[]): void {
    doctors.forEach(d => {
      this.doctorService.getAverageRating(d.id).subscribe({
        next: (avg) => { this.doctorRatings[d.id] = avg ?? null; this.cdr.detectChanges(); },
        error: () => { this.doctorRatings[d.id] = null; this.cdr.detectChanges(); }
      });
    });
  }
}
