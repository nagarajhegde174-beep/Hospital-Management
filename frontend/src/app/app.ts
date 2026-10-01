import { Component, OnInit } from '@angular/core';
import { AuthService } from './core/auth/auth.service';
import { UserProfileService } from './core/services/user-profile.service';

@Component({
  selector: 'app-root',
  standalone: false,
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  constructor(
    private auth: AuthService,
    private userProfile: UserProfileService
  ) {}

  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      this.userProfile.loadName();
    }
  }
}
