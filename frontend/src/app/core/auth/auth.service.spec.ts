import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService, LoginResponse } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {

  let service: AuthService;
  let httpMock: HttpTestingController;
  const loginUrl = `${environment.gatewayUrl}/auth-service/api/v1/auth/login`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('should store token on login', () => {
    service.login({ email: 'p@test.com', password: '1234' }).subscribe();
    httpMock.expectOne(loginUrl).flush({ token: 'jwt', userId: 1, serviceId: 1, role: 'ROLE_PATIENT' } as LoginResponse);
    expect(localStorage.getItem('hams_token_patient')).toBe('jwt');
  });

  it('should return null when no token exists', () => {
    expect(service.getToken()).toBeNull();
  });

  it('should remove only current role token on logout', () => {
    localStorage.setItem('hams_token_patient', 'p-jwt');
    localStorage.setItem('hams_token_doctor', 'd-jwt');
    sessionStorage.setItem('hams_active_role', 'ROLE_PATIENT');
    service.logout();
    expect(localStorage.getItem('hams_token_patient')).toBeNull();
    expect(localStorage.getItem('hams_token_doctor')).toBe('d-jwt');
  });

});
