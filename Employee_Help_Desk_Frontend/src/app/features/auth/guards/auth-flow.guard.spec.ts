import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { PasswordResetFlowService } from '../services/password-reset-flow.service';
import { resetPasswordGuard, verifyOtpGuard } from './auth-flow.guard';

describe('Auth Flow Guards', () => {
  let flowService: PasswordResetFlowService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([])]
    });
    flowService = TestBed.inject(PasswordResetFlowService);
    router = TestBed.inject(Router);
  });

  describe('verifyOtpGuard', () => {
    it('should redirect to /forgot-password if no session is active', () => {
      const result = TestBed.runInInjectionContext(() => verifyOtpGuard({} as any, {} as any));
      expect(result instanceof UrlTree).toBe(true);
      expect((result as UrlTree).toString()).toBe('/forgot-password');
    });

    it('should allow access if session is active with valid email', () => {
      flowService.startResetSession('user@example.com');
      const result = TestBed.runInInjectionContext(() => verifyOtpGuard({} as any, {} as any));
      expect(result).toBe(true);
    });
  });

  describe('resetPasswordGuard', () => {
    it('should redirect to /forgot-password if OTP is not verified', () => {
      flowService.startResetSession('user@example.com');
      const result = TestBed.runInInjectionContext(() => resetPasswordGuard({} as any, {} as any));
      expect(result instanceof UrlTree).toBe(true);
      expect((result as UrlTree).toString()).toBe('/forgot-password');
    });

    it('should allow access if OTP has been verified', () => {
      flowService.startResetSession('user@example.com');
      flowService.verifyOtp('123456');
      const result = TestBed.runInInjectionContext(() => resetPasswordGuard({} as any, {} as any));
      expect(result).toBe(true);
    });
  });
});
