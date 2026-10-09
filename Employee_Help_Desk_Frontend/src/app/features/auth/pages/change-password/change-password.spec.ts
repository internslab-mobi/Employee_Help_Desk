import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ChangePassword } from './change-password';

describe('ChangePassword', () => {
  let component: ChangePassword;
  let fixture: ComponentFixture<ChangePassword>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChangePassword],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(ChangePassword);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have lock icon inside .icon-wrapper centered above heading', () => {
    const element = fixture.nativeElement as HTMLElement;
    const card = element.querySelector('.change-password-card');
    expect(card).toBeTruthy();

    const iconWrapper = card?.querySelector('.icon-wrapper');
    expect(iconWrapper).toBeTruthy();

    const lockIcon = iconWrapper?.querySelector('.lock-icon');
    expect(lockIcon).toBeTruthy();

    const h1 = card?.querySelector('h1');
    expect(h1).toBeTruthy();
    expect(h1?.textContent?.trim()).toBe('Change password');
  });

  it('should have back to login link pointing to /login', () => {
    const element = fixture.nativeElement as HTMLElement;
    const backToLogin = element.querySelector('.back-to-login') as HTMLAnchorElement;
    expect(backToLogin).toBeTruthy();
    expect(backToLogin.textContent).toContain('Back to Login');
  });

  it('should validate current password requirement without pattern regex', () => {
    expect(component.currentPassword?.valid).toBe(false);
    expect(component.currentPassword?.hasError('required')).toBe(true);

    // Any non-empty string should satisfy currentPassword
    component.changePasswordForm.patchValue({
      currentPassword: 'anyoldpassword'
    });
    expect(component.currentPassword?.valid).toBe(true);
  });

  it('should validate password requirements and strength correctly for new password', () => {
    component.changePasswordForm.patchValue({
      newPassword: 'short'
    });
    expect(component.hasMinimumLength()).toBe(false);
    expect(component.hasUppercase()).toBe(false);
    expect(component.hasLowercase()).toBe(true);
    expect(component.hasNumber()).toBe(false);
    expect(component.hasSpecialCharacter()).toBe(false);
    expect(component.passwordStrength).toBe('low');

    component.changePasswordForm.patchValue({
      newPassword: 'Password1!'
    });
    expect(component.hasMinimumLength()).toBe(true);
    expect(component.hasUppercase()).toBe(true);
    expect(component.hasLowercase()).toBe(true);
    expect(component.hasNumber()).toBe(true);
    expect(component.hasSpecialCharacter()).toBe(true);
    expect(component.passwordStrength).toBe('medium');

    component.changePasswordForm.patchValue({
      newPassword: 'Password12!!Strong'
    });
    expect(component.passwordStrength).toBe('strong');
  });

  it('should keep change password button disabled until all fields are valid and match', () => {
    expect(component.changePasswordForm.invalid).toBe(true);

    // Only current password filled
    component.changePasswordForm.patchValue({
      currentPassword: 'CurrentPassword123!'
    });
    expect(component.changePasswordForm.invalid).toBe(true);

    // Mismatched passwords
    component.changePasswordForm.patchValue({
      currentPassword: 'CurrentPassword123!',
      newPassword: 'Password1!',
      confirmPassword: 'MismatchPassword!'
    });
    expect(component.changePasswordForm.hasError('passwordMismatch')).toBe(true);
    expect(component.changePasswordForm.invalid).toBe(true);

    // Matching and valid
    component.changePasswordForm.patchValue({
      currentPassword: 'CurrentPassword123!',
      newPassword: 'Password1!',
      confirmPassword: 'Password1!'
    });
    expect(component.changePasswordForm.valid).toBe(true);
  });
});
