import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { ForgotPasswordComponent } from './forgot-password.component';
import { PasswordResetFlowService } from '../../services/password-reset-flow.service';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let flowService: PasswordResetFlowService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    flowService = TestBed.inject(PasswordResetFlowService);
    router = TestBed.inject(Router);

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should display "Send OTP" button', () => {
    const el = fixture.nativeElement as HTMLElement;
    const submitBtn = el.querySelector('.btn-submit');
    expect(submitBtn).toBeTruthy();
    expect(submitBtn?.textContent).toContain('Send OTP');
  });

  it('should validate invalid email and display error message', () => {
    component.forgotPasswordForm.setValue({ email: 'invalid-email' });
    component.onSubmit();
    fixture.detectChanges();

    expect(component.forgotPasswordForm.invalid).toBe(true);
    expect(component.emailErrorMessage).toContain('Please enter a valid work email address.');
  });

  it('should validate valid email, start reset session, and navigate to /verify-otp', async () => {
    vi.useFakeTimers();
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.forgotPasswordForm.setValue({ email: 'alex@company.com' });
    component.onSubmit();

    expect(flowService.email()).toBe('alex@company.com');
    expect(flowService.remainingAttempts()).toBe(3);

    vi.advanceTimersByTime(500);
    fixture.detectChanges();
    expect(component.isSubmittedSuccess()).toBe(true);

    vi.advanceTimersByTime(600);
    expect(navigateSpy).toHaveBeenCalledWith(['/verify-otp']);

    vi.useRealTimers();
  });
});
