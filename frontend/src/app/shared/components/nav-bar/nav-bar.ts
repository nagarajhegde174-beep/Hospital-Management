import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import { UserProfileService } from '../../../core/services/user-profile.service';

@Component({
  selector: 'app-nav-bar',
  standalone: false,
  templateUrl: './nav-bar.html',
  styleUrl: './nav-bar.css'
})
export class NavBar implements OnInit, OnDestroy {
  currentUrl = '';
  menuOpen = false;
  displayName = '';
  private routerSub!: Subscription;
  private nameSub!: Subscription;

  constructor(
    public auth: AuthService,
    private userProfile: UserProfileService,
    private router: Router,
    private cdr: ChangeDetectorRef  // ← ADD
  ) {}

  ngOnInit(): void {
    this.currentUrl = this.router.url;
    this.routerSub = this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: NavigationEnd) => {
        this.currentUrl = e.urlAfterRedirects;
        this.menuOpen = false;
      });

    this.nameSub = this.userProfile.displayName$.subscribe(name => {
      this.displayName = name;
      this.cdr.detectChanges();  // ← ADD
    });

    if (this.auth.isLoggedIn()) {
      this.userProfile.loadName();  // ← keeps this
    }
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
    this.nameSub?.unsubscribe();
  }

  get hideNav(): boolean {
    return this.currentUrl === '/' ||
           this.currentUrl.startsWith('/login') ||
           this.currentUrl.startsWith('/register');
  }

  get isPatient(): boolean { return this.auth.getRole() === 'ROLE_PATIENT'; }
  get isDoctor(): boolean  { return this.auth.getRole() === 'ROLE_DOCTOR'; }
  get isAdmin(): boolean   { return this.auth.getRole() === 'ROLE_ADMIN'; }

  toggleMenu(): void { this.menuOpen = !this.menuOpen; }
  closeMenu(): void  { this.menuOpen = false; }

  logout(): void {
    this.menuOpen = false;
    this.auth.logout();
  }
}