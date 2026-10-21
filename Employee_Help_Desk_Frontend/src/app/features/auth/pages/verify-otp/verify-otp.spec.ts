import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { VerifyOtp } from './verify-otp';
import { PasswordResetFlowService } from '../../services/password-reset-flow.service';

describe('VerifyOtp Component', () => {
  let component: VerifyOtp;
  let fixture: ComponentFixture<VerifyOtp>;
  let flowService: PasswordResetFlowService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerifyOtp],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    flowService = TestBed.inject(PasswordResetFlowService);
    router = TestBed.inject(Router);

    // Initialize mock session
    flowService.resetSession('jane.doe@company.com');

    fixture = TestBed.createComponent(VerifyOtp);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    component.ngOnDestroy();
  });

  it('should create the VerifyOtp component', () => {
    expect(component).toBeTruthy();
  });

  it('should display the masked email', () => {
    const el = fixture.nativeElement as HTMLElement;
    const highlight = el.querySelector('.highlight-email');
    expect(highlight).toBeTruthy();
    expect(highlight?.textContent?.trim()).toBe('j***e@company.com');
  });

  it('should render six OTP input boxes', () => {
    const el = fixture.nativeElement as HTMLElement;
    const boxes = el.querySelectorAll('.otp-digit-box');
    expect(boxes.length).toBe(6);
  });

  it('should display initial 3 attempts remaining', () => {
    const el = fixture.nativeElement as HTMLElement;
    const indicator = el.querySelector('.attempts-indicator');
    expect(indicator?.textContent).toContain('3 of 3 verification attempts');
  });

  it('should have back to forgot password link', () => {
    const el = fixture.nativeElement as HTMLElement;
    const backLink = el.querySelector('.back-to-forgot') as HTMLAnchorElement;
    expect(backLink).toBeTruthy();
    expect(backLink.textContent).toContain('Back to Forgot Password');
  });

  it('should keep verify button disabled until all 6 digits entered', () => {
    const el = fixture.nativeElement as HTMLElement;
    const verifyBtn = el.querySelector('.btn-submit') as HTMLButtonElement;
    expect(verifyBtn.disabled).toBe(true);

    component.otpDigits.set(['1', '2', '3', '4', '5', '']);
    fixture.detectChanges();
    expect(verifyBtn.disabled).toBe(true);

    component.otpDigits.set(['1', '2', '3', '4', '5', '6']);
    fixture.detectChanges();
    expect(verifyBtn.disabled).toBe(false);
  });

  it('should handle incorrect OTP verification and decrease attempts', async () => {
    vi.useFakeTimers();

    vi.spyOn(flowService, 'verifyOtp').mockImplementation(() => {
      flowService['attemptsSignal'].set(2);
      return throwError(() => ({ error: { message: 'Incorrect OTP' } }));
    });

    component.otpDigits.set(['9', '9', '9', '9', '9', '9']);
    component.onVerify();

    vi.advanceTimersByTime(500);
    fixture.detectChanges();

    expect(component.errorMessage()).toContain('Incorrect OTP');
    expect(component.remainingAttempts()).toBe(2);

    vi.useRealTimers();
  });

  it('should disable verification and show locked state after 3 failed attempts', async () => {
    vi.useFakeTimers();

    vi.spyOn(flowService, 'verifyOtp').mockImplementation(() => {
      flowService['attemptsSignal'].set(0);
      return throwError(() => ({ error: { message: 'max retry exceeded' } }));
    });

    component.otpDigits.set(['1', '1', '1', '1', '1', '1']);
    component.onVerify();
    vi.advanceTimersByTime(500);

    fixture.detectChanges();

    expect(component.remainingAttempts()).toBe(0);
    expect(component.isLocked()).toBe(true);
    expect(component.errorMessage()).toContain('max retry exceeded');

    const el = fixture.nativeElement as HTMLElement;
    const verifyBtn = el.querySelector('.btn-submit') as HTMLButtonElement;
    expect(verifyBtn.disabled).toBe(true);

    vi.useRealTimers();
  });

  it('should redirect to /forgot-password when retries are exceeded', async () => {
    vi.useFakeTimers();
    const navigateSpy = vi.spyOn(router, 'navigate');

    vi.spyOn(flowService, 'verifyOtp').mockImplementation(() => {
      flowService['attemptsSignal'].set(0);
      return throwError(() => ({ error: { message: 'max retry exceeded' } }));
    });

    component.otpDigits.set(['1', '1', '1', '1', '1', '1']);
    component.onVerify();
    vi.advanceTimersByTime(500);

    expect(component.errorMessage()).toBe('max retry exceeded');

    // Wait for redirect timeout
    vi.advanceTimersByTime(1600);
    expect(navigateSpy).toHaveBeenCalledWith(['/forgot-password']);

    vi.useRealTimers();
  });

  it('should navigate to /reset-password upon entering 123456', async () => {
    vi.useFakeTimers();
    const navigateSpy = vi.spyOn(router, 'navigate');

    vi.spyOn(flowService, 'verifyOtp').mockReturnValue(
      of({ resetToken: 'mock-valid-token' })
    );

    component.otpDigits.set(['1', '2', '3', '4', '5', '6']);
    component.onVerify();

    vi.advanceTimersByTime(500);
    expect(component.successMessage()).toBe('OTP verified successfully!');

    vi.advanceTimersByTime(700);
    expect(navigateSpy).toHaveBeenCalledWith(['/reset-password']);

    vi.useRealTimers();
  });
});
