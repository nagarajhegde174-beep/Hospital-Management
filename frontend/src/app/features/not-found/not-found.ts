import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-not-found',
  standalone: false,
  templateUrl: './not-found.html',
  styleUrl: './not-found.css'
})
export class NotFound {
  constructor(private router: Router, private auth: AuthService) {}

  goHome(): void {
    const role = this.auth.getRole();
    if (role === 'ROLE_DOCTOR')  this.router.navigate(['/doctor/dashboard']);
    else if (role === 'ROLE_PATIENT') this.router.navigate(['/patient/dashboard']);
    else if (role === 'ROLE_ADMIN')   this.router.navigate(['/admin/dashboard']);
    else this.router.navigate(['/login']);
  }
}
