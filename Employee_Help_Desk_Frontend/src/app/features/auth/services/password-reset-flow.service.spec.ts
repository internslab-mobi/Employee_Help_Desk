import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { PasswordResetFlowService } from './password-reset-flow.service';

describe('PasswordResetFlowService', () => {
  let service: PasswordResetFlowService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PasswordResetFlowService);
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

  it('should verify successfully with mock OTP 123456', () => {
    service.startResetSession('test@example.com');
    const result = service.verifyOtp('123456');

    expect(result.success).toBe(true);
    expect(result.message).toBe('OTP verified successfully!');
    expect(service.isOtpVerified()).toBe(true);
    expect(service.canAccessResetPassword()).toBe(true);
  });

  it('should decrement attempts on incorrect OTP and lock after 3 attempts', () => {
    service.startResetSession('test@example.com');

    // Attempt 1
    const res1 = service.verifyOtp('000000');
    expect(res1.success).toBe(false);
    expect(res1.remainingAttempts).toBe(2);
    expect(res1.isLocked).toBe(false);
    expect(service.isOtpVerified()).toBe(false);

    // Attempt 2
    const res2 = service.verifyOtp('111111');
    expect(res2.success).toBe(false);
    expect(res2.remainingAttempts).toBe(1);
    expect(res2.isLocked).toBe(false);

    // Attempt 3
    const res3 = service.verifyOtp('999999');
    expect(res3.success).toBe(false);
    expect(res3.remainingAttempts).toBe(0);
    expect(res3.isLocked).toBe(true);
    expect(service.remainingAttempts()).toBe(0);

    // Attempt 4 while locked
    const res4 = service.verifyOtp('123456');
    expect(res4.success).toBe(false);
    expect(res4.isLocked).toBe(true);
  });

  it('should reset attempts and unlock session on resendOtp', () => {
    service.startResetSession('test@example.com');
    service.verifyOtp('000000');
    service.verifyOtp('000000');
    service.verifyOtp('000000');
    expect(service.remainingAttempts()).toBe(0);

    service.resendOtp();
    expect(service.remainingAttempts()).toBe(3);
    expect(service.isOtpVerified()).toBe(false);

    // Now verification should work again
    const res = service.verifyOtp('123456');
    expect(res.success).toBe(true);
  });

  it('should clear all state on completeReset', () => {
    service.startResetSession('test@example.com');
    service.verifyOtp('123456');
    expect(service.canAccessResetPassword()).toBe(true);

    service.completeReset();
    expect(service.email()).toBe('');
    expect(service.isOtpVerified()).toBe(false);
    expect(service.canAccessVerifyOtp()).toBe(false);
    expect(service.canAccessResetPassword()).toBe(false);
  });
});
