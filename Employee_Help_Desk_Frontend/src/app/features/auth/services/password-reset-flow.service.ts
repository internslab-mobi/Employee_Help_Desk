import { Injectable, signal, computed, inject } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/config/api.config';
import { AuthApiResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  VerifyOtpRequest,
  VerifyOtpResponse 
} from '../../../core/models/auth.models';

export interface VerifyOtpResult {
  success: boolean;
  message: string;
  remainingAttempts: number;
  isLocked: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class PasswordResetFlowService {
  private readonly http = inject(HttpClient);
  
  readonly MAX_ATTEMPTS = 3;
  readonly OTP_VALIDITY_SECONDS = 120; // 2 minutes

  private readonly emailSignal = signal<string>('');
  private readonly attemptsSignal = signal<number>(this.MAX_ATTEMPTS);
  private readonly isOtpVerifiedSignal = signal<boolean>(false);
  private readonly isSessionActiveSignal = signal<boolean>(false);
  private readonly sessionStartSignal = signal<number>(0);
  private readonly resetTokenSignal = signal<string>('');

  readonly email = this.emailSignal.asReadonly();
  readonly remainingAttempts = this.attemptsSignal.asReadonly();
  readonly isOtpVerified = this.isOtpVerifiedSignal.asReadonly();
  readonly isSessionActive = this.isSessionActiveSignal.asReadonly();
  readonly resetToken = this.resetTokenSignal.asReadonly();

  readonly maskedEmail = computed(() => {
    return this.maskEmail(this.emailSignal());
  });

  /**
   * Send OTP - Calls backend POST /api/user/forgot-password/request-otp
  */
  requestOtp(email: string): Observable<AuthApiResponse>{
    const payload: ForgotPasswordRequest = {email: email.trim()};

    return this.http.post<AuthApiResponse>(API_ENDPOINTS.auth.forgotPasswordRequestOtp, payload).pipe(
      tap(() => {
        this.emailSignal.set(email.trim());
        this.attemptsSignal.set(this.MAX_ATTEMPTS);
        this.isOtpVerifiedSignal.set(false);
        this.isSessionActiveSignal.set(true);
        this.resetTokenSignal.set('');
        this.sessionStartSignal.set(Date.now());
      })
    );
  }

  /**
   * Verify OTP - Calls backend POST /api/user/forgot-password/verify-otp
  */
  verifyOtp(otp: string): Observable<VerifyOtpResponse> {
    const payload: VerifyOtpRequest = {
      email: this.emailSignal(),
      otp: otp.trim()
    };

    return this.http.post<VerifyOtpResponse>(API_ENDPOINTS.auth.forgotPasswordVerifyOtp, payload).pipe(
      tap((res) => {
        this.isOtpVerifiedSignal.set(true);
        this.resetTokenSignal.set(res.resetToken);
      }),
      catchError((err) => {
        const currentAttempts = this.attemptsSignal();
        const updated = Math.max(0, currentAttempts - 1);
        this.attemptsSignal.set(updated);
        return throwError(() => err);
      })
    );
  }

  /**
   * Rest Password - Calls backend POST /api/user/forgot-password/reset-password
   */
  resetPassword(newPassword: string, confirmNewPassword: string): Observable<AuthApiResponse>{
    const payload: ResetPasswordRequest = {
      email: this.emailSignal(),
      resetToken: this.resetTokenSignal(),
      newPassword,
      confirmNewPassword
    };

    return this.http.post<AuthApiResponse>(API_ENDPOINTS.auth.forgotPasswordResetPassword, payload).pipe(
      tap(() => {
        this.completeReset();
      })
    )
  }

  /**
   * Resend OTP: triggers a new OTP dispatch from backend
   */
  resendOtp(): Observable<AuthApiResponse> {
    return this.requestOtp(this.emailSignal());
  }

  /**
   * Checks if the 2-minute OTP period expired locally
   */
  isOtpExpired(): boolean {
    const start = this.sessionStartSignal();
    if (!start) return false;
    return (Date.now() - start) > this.OTP_VALIDITY_SECONDS * 1000;
  }

  /**
   * Completes the reset process and clears all in-memory temporary state.
   */
  completeReset(): void {
    this.resetSession();
  }

  /**
   * Resets all in-memory flow state.
   */
  resetSession(): void {
    this.emailSignal.set('');
    this.attemptsSignal.set(this.MAX_ATTEMPTS);
    this.isOtpVerifiedSignal.set(false);
    this.isSessionActiveSignal.set(false);
    this.resetTokenSignal.set('');
    this.sessionStartSignal.set(0);
  }

  /**
   * Mask an email address (e.g., john.doe@company.com -> j***e@company.com).
   */
  maskEmail(email: string): string {
    if (!email || !email.includes('@')) {
      return email || '';
    }

    const [user, domain] = email.split('@');
    if (!user || !domain) {
      return email;
    }

    if (user.length <= 1) {
      return `${user}***@${domain}`;
    }

    if (user.length === 2) {
      return `${user[0]}***@${domain}`;
    }

    return `${user[0]}***${user[user.length - 1]}@${domain}`;
  }

  canAccessVerifyOtp(): boolean {
    return this.isSessionActiveSignal() && this.emailSignal().length > 0;
  }

  canAccessResetPassword(): boolean {
    return this.isOtpVerifiedSignal() && this.resetTokenSignal().length > 0  && this.emailSignal().length > 0;
  }
}
