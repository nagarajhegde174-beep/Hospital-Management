import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {

  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    if (!this.auth.isLoggedIn()) {
      return this.router.createUrlTree(['/login'], {
        queryParams: { reason: 'auth-required' }
      });
    }

    const requiredRole: string = route.data['role'];
    const userRole = this.auth.getRole();

    if (!requiredRole || userRole === requiredRole) {
      return true;
    }

    // Logged in but wrong role — send to their own dashboard, not login
    if (userRole === 'ROLE_PATIENT') return this.router.createUrlTree(['/patient/dashboard']);
    if (userRole === 'ROLE_DOCTOR')  return this.router.createUrlTree(['/doctor/dashboard']);
    if (userRole === 'ROLE_ADMIN')   return this.router.createUrlTree(['/admin/doctors']);

    return this.router.createUrlTree(['/login'], { queryParams: { reason: 'forbidden' } });
  }
}
