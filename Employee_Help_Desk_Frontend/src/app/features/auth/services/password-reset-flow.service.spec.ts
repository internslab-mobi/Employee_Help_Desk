import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PasswordResetFlowService } from './password-reset-flow.service';
import { API_ENDPOINTS } from '../../../core/config/api.config';

describe('PasswordResetFlowService', () => {
  let service: PasswordResetFlowService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(PasswordResetFlowService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should properly mask email addresses', () => {
    expect(service.maskEmail('john.doe@example.com')).toBe('j***e@example.com');
    expect(service.maskEmail('roshini.b@company.com')).toBe('r***b@company.com');
    expect(service.maskEmail('ab@domain.com')).toBe('a***@domain.com');
    expect(service.maskEmail('a@domain.com')).toBe('a***@domain.com');
    expect(service.maskEmail('')).toBe('');
  });

  it('should start reset session with 3 attempts and inactive verification', () => {
    service.startResetSession('test@example.com');
    expect(service.email()).toBe('test@example.com');
    expect(service.remainingAttempts()).toBe(3);
    expect(service.isOtpVerified()).toBe(false);
    expect(service.canAccessVerifyOtp()).toBe(true);
    expect(service.canAccessResetPassword()).toBe(false);
    expect(service.maskedEmail()).toBe('t***t@example.com');
  });

  it('should verify successfully with OTP via HTTP', () => {
    service.startResetSession('test@example.com');

    service.verifyOtp('123456').subscribe((res) => {
      expect(res.resetToken).toBe('mock-reset-token');
      expect(service.isOtpVerified()).toBe(true);
      expect(service.canAccessResetPassword()).toBe(true);
    });

    const req = httpMock.expectOne(API_ENDPOINTS.auth.forgotPasswordVerifyOtp);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'test@example.com', otp: '123456' });
    req.flush({ resetToken: 'mock-reset-token' });
  });

  it('should decrement attempts on incorrect OTP error', () => {
    service.startResetSession('test@example.com');
    expect(service.remainingAttempts()).toBe(3);

    // Attempt 1 fails
    service.verifyOtp('000000').subscribe({
      error: () => {
        expect(service.remainingAttempts()).toBe(2);
      }
    });

    const req1 = httpMock.expectOne(API_ENDPOINTS.auth.forgotPasswordVerifyOtp);
    req1.flush({ message: 'Invalid OTP' }, { status: 400, statusText: 'Bad Request' });

    // Attempt 2 fails
    service.verifyOtp('111111').subscribe({
      error: () => {
        expect(service.remainingAttempts()).toBe(1);
      }
    });

    const req2 = httpMock.expectOne(API_ENDPOINTS.auth.forgotPasswordVerifyOtp);
    req2.flush({ message: 'Invalid OTP' }, { status: 400, statusText: 'Bad Request' });

    // Attempt 3 fails
    service.verifyOtp('999999').subscribe({
      error: () => {
        expect(service.remainingAttempts()).toBe(0);
      }
    });

    const req3 = httpMock.expectOne(API_ENDPOINTS.auth.forgotPasswordVerifyOtp);
    req3.flush({ message: 'Maximum retry exceeded' }, { status: 400, statusText: 'Bad Request' });
  });

  it('should reset attempts and send request on resendOtp', () => {
    service.startResetSession('test@example.com');

    service.resendOtp().subscribe((res) => {
      expect(res.message).toBe('OTP sent successfully');
      expect(service.remainingAttempts()).toBe(3);
      expect(service.isOtpVerified()).toBe(false);
    });

    const req = httpMock.expectOne(API_ENDPOINTS.auth.forgotPasswordRequestOtp);
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'OTP sent successfully' });
  });

  it('should clear all state on completeReset', () => {
    service.startResetSession('test@example.com');

    service.completeReset();
    expect(service.email()).toBe('');
    expect(service.isOtpVerified()).toBe(false);
    expect(service.canAccessVerifyOtp()).toBe(false);
    expect(service.canAccessResetPassword()).toBe(false);
  });
});
