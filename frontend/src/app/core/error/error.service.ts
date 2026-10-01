import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';

export interface AppError {
  message: string;
  status?: number;
}

@Injectable({ providedIn: 'root' })
export class ErrorService {
  private errorSubject = new BehaviorSubject<AppError | null>(null);

  error$: Observable<AppError | null> = this.errorSubject.asObservable();

  extractErrorMessage(err: HttpErrorResponse): string {
    return (
      err.error?.message ??
      err.error?.error ??
      (typeof err.error === 'string' ? err.error : null) ??
      err.message ??
      'An unknown error occurred'
    );
  }

  showError(message: string, status?: number): void {
    this.errorSubject.next({ message, status });
  }

  clearError(): void {
    this.errorSubject.next(null);
  }

  handleHttpError(err: HttpErrorResponse): string {
    const message = this.extractErrorMessage(err);

    switch (err.status) {
      case 400:
        return message;
      case 401:
        return 'Your session has expired. Please log in again.';
      case 403:
        return 'You do not have permission to perform this action.';
      case 404:
        return 'The requested resource was not found.';
      case 409:
        return message || 'A conflict occurred. Please try again.';
      case 500:
        return 'Something went wrong on the server. Please try again later.';
      default:
        return message;
    }
  }
}
