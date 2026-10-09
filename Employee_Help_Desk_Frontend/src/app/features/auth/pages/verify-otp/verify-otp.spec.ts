import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
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
      providers: [provideRouter([])]
    }).compileComponents();

    flowService = TestBed.inject(PasswordResetFlowService);
    router = TestBed.inject(Router);

    // Initialize mock session
    flowService.startResetSession('jane.doe@company.com');

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

  it('should display the mock OTP dev hint', () => {
    const el = fixture.nativeElement as HTMLElement;
    const hint = el.querySelector('.mock-otp-badge');
    expect(hint).toBeTruthy();
    expect(hint?.textContent).toContain('123456');
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

    // 1st attempt
    component.otpDigits.set(['1', '1', '1', '1', '1', '1']);
    component.onVerify();
    vi.advanceTimersByTime(500);

    // 2nd attempt
    component.otpDigits.set(['2', '2', '2', '2', '2', '2']);
    component.onVerify();
    vi.advanceTimersByTime(500);

    // 3rd attempt
    component.otpDigits.set(['3', '3', '3', '3', '3', '3']);
    component.onVerify();
    vi.advanceTimersByTime(500);

    fixture.detectChanges();

    expect(component.remainingAttempts()).toBe(0);
    expect(component.isLocked()).toBe(true);
    expect(component.errorMessage()).toContain('exhausted all 3 attempts');

    const el = fixture.nativeElement as HTMLElement;
    const verifyBtn = el.querySelector('.btn-submit') as HTMLButtonElement;
    expect(verifyBtn.disabled).toBe(true);

    vi.useRealTimers();
  });

  it('should navigate to /reset-password upon entering 123456', async () => {
    vi.useFakeTimers();
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.otpDigits.set(['1', '2', '3', '4', '5', '6']);
    component.onVerify();

    vi.advanceTimersByTime(500);
    expect(component.successMessage()).toBe('OTP verified successfully!');

    vi.advanceTimersByTime(700);
    expect(navigateSpy).toHaveBeenCalledWith(['/reset-password']);

    vi.useRealTimers();
  });
});
