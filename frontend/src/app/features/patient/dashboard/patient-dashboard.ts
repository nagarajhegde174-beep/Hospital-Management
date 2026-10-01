import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { UserProfileService } from '../../../core/services/user-profile.service';

@Component({
  selector: 'app-patient-dashboard',
  standalone: false,
  templateUrl: './patient-dashboard.html',
  styleUrl: './patient-dashboard.css'
})
export class PatientDashboard implements OnInit, OnDestroy {
  displayName = '';
  todayLabel = '';
  private sub: Subscription | null = null;

  constructor(
    private userProfile: UserProfileService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.todayLabel = new Date().toLocaleDateString(undefined, {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    });
    this.sub = this.userProfile.displayName$.subscribe(name => {
      this.displayName = name;
      this.cdr.detectChanges();
    });
    this.userProfile.loadName();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}