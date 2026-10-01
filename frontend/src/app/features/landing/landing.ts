import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-landing',
  standalone: false,
  templateUrl: './landing.html',
  styleUrl: './landing.css'
})
export class Landing implements OnInit {
  constructor(private auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      const role = this.auth.getRole();
      if (role === 'ROLE_PATIENT') this.router.navigate(['/patient/dashboard']);
      else if (role === 'ROLE_DOCTOR') this.router.navigate(['/doctor/dashboard']);
      else if (role === 'ROLE_ADMIN') this.router.navigate(['/admin/doctors']);
    }
  }
}
