import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, Subject, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../../environments/environment';
import { JwtPayload } from '../../shared/models/jwt-payload.model';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  serviceId: number;
  role: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role: string;
  dateOfBirth?: string;
  gender?: string;
  contactDetails: string;
  specialization?: string;
  designation?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly PATIENT_KEY = 'hams_token_patient';
  private readonly DOCTOR_KEY = 'hams_token_doctor';
  private readonly ADMIN_KEY = 'hams_token_admin';
  private readonly ACTIVE_ROLE = 'hams_active_role';
  private readonly base = `${environment.gatewayUrl}/auth-service/api/v1/auth`;

  private logoutSubject = new Subject<void>();
  readonly logout$ = this.logoutSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) { }

  private keyForRole(role: string): string {
    if (role === 'DOCTOR' || role === 'ROLE_DOCTOR') return this.DOCTOR_KEY;
    if (role === 'ADMIN' || role === 'ROLE_ADMIN') return this.ADMIN_KEY;
    return this.PATIENT_KEY;
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/login`, credentials).pipe(
      tap(res => {
        localStorage.setItem(this.keyForRole(res.role), res.token);
        sessionStorage.setItem(this.ACTIVE_ROLE, res.role);
      })
    );
  }

  register(payload: RegisterRequest): Observable<string> {
    return this.http.post(`${this.base}/register`, payload, { responseType: 'text' });
  }

  logout(): void {
    const role = sessionStorage.getItem(this.ACTIVE_ROLE);
    if (role) {
      localStorage.removeItem(this.keyForRole(role));
    }
    sessionStorage.removeItem(this.ACTIVE_ROLE);
    this.logoutSubject.next();
    this.router.navigate(['/']);
  }

  getToken(): string | null {
    const role = sessionStorage.getItem(this.ACTIVE_ROLE);
    if (role) return localStorage.getItem(this.keyForRole(role));
    // Fallback on hard refresh with no sessionStorage: pick whichever key exists
    return localStorage.getItem(this.DOCTOR_KEY)
      ?? localStorage.getItem(this.PATIENT_KEY)
      ?? localStorage.getItem(this.ADMIN_KEY);
  }

  getDecodedPayload(): JwtPayload | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      return jwtDecode<JwtPayload>(token);
    } catch {
      return null;
    }
  }

  getServiceId(): number | null {
    return this.getDecodedPayload()?.serviceId ?? null;
  }

  getRole(): string | null {
    return this.getDecodedPayload()?.role ?? null;
  }

  getUserName(): string | null {
    return this.getDecodedPayload()?.sub ?? null;
  }

  isLoggedIn(): boolean {
    const payload = this.getDecodedPayload();
    if (!payload) return false;
    return payload.exp * 1000 > Date.now();
  }

  isTokenExpired(): boolean {
    return !this.isLoggedIn();
  }
  getPendingDoctors(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/users/pending-doctors`);
  }

  approveDoctor(userId: number): Observable<string> {
    return this.http.patch(`${this.base}/users/${userId}/approve`, {}, { responseType: 'text' });
  }

  rejectDoctor(userId: number): Observable<string> {
    return this.http.patch(`${this.base}/users/${userId}/reject`, {}, { responseType: 'text' });
  }

  sendOtp(email: string): Observable<string> {
    return this.http.post(`${this.base}/send-otp?email=${encodeURIComponent(email)}`, {}, { responseType: 'text' });
  }

  verifyOtpAndReset(email: string, otp: string, newPassword: string): Observable<string> {
    return this.http.post(
      `${this.base}/verify-otp-reset?email=${encodeURIComponent(email)}&otp=${encodeURIComponent(otp)}&newPassword=${encodeURIComponent(newPassword)}`,
      {}, { responseType: 'text' });
  }
  verifyOtp(email: string, otp: string): Observable<string> {
    return this.http.post(
      `${this.base}/verify-otp?email=${encodeURIComponent(email)}&otp=${encodeURIComponent(otp)}`,
      {}, { responseType: 'text' });
  }

}
