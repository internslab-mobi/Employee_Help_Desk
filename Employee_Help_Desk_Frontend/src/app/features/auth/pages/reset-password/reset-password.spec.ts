import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ResetPassword } from './reset-password';

describe('ResetPassword', () => {
  let component: ResetPassword;
  let fixture: ComponentFixture<ResetPassword>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResetPassword],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPassword);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have lock icon inside .icon-wrapper centered above heading', () => {
    const element = fixture.nativeElement as HTMLElement;
    const card = element.querySelector('.reset-password-card');
    expect(card).toBeTruthy();

    const iconWrapper = card?.querySelector('.icon-wrapper');
    expect(iconWrapper).toBeTruthy();

    const lockIcon = iconWrapper?.querySelector('.lock-icon');
    expect(lockIcon).toBeTruthy();

    const h1 = card?.querySelector('h1');
    expect(h1).toBeTruthy();
    expect(h1?.textContent?.trim()).toBe('Reset password');
  });

  it('should have back to login link pointing to /login', () => {
    const element = fixture.nativeElement as HTMLElement;
    const backToLogin = element.querySelector('.back-to-login') as HTMLAnchorElement;
    expect(backToLogin).toBeTruthy();
    expect(backToLogin.textContent).toContain('Back to Login');
  });

  it('should validate password requirements and strength correctly', () => {
    component.resetPasswordForm.patchValue({
      newPassword: 'short'
    });
    expect(component.hasMinimumLength()).toBe(false);
    expect(component.hasUppercase()).toBe(false);
    expect(component.hasLowercase()).toBe(true);
    expect(component.hasNumber()).toBe(false);
    expect(component.hasSpecialCharacter()).toBe(false);
    expect(component.passwordStrength).toBe('low');

    component.resetPasswordForm.patchValue({
      newPassword: 'Password1!'
    });
    expect(component.hasMinimumLength()).toBe(true);
    expect(component.hasUppercase()).toBe(true);
    expect(component.hasLowercase()).toBe(true);
    expect(component.hasNumber()).toBe(true);
    expect(component.hasSpecialCharacter()).toBe(true);
    expect(component.passwordStrength).toBe('medium');

    component.resetPasswordForm.patchValue({
      newPassword: 'Password12!!Strong'
    });
    expect(component.passwordStrength).toBe('strong');
  });

  it('should keep reset password button disabled until form is valid', () => {
    expect(component.resetPasswordForm.invalid).toBe(true);

    component.resetPasswordForm.patchValue({
      newPassword: 'Password1!',
      confirmPassword: 'MismatchPassword!'
    });
    expect(component.resetPasswordForm.hasError('passwordMismatch')).toBe(true);
    expect(component.resetPasswordForm.invalid).toBe(true);

    component.resetPasswordForm.patchValue({
      newPassword: 'Password1!',
      confirmPassword: 'Password1!'
    });
    expect(component.resetPasswordForm.valid).toBe(true);
  });
});
