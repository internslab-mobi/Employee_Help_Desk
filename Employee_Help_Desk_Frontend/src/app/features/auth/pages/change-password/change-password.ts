import { Component } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './change-password.html',
  styleUrl: './change-password.css'
})
export class ChangePassword {
  changePasswordForm: FormGroup;

  passwordStrength: 'low' | 'medium' | 'strong' | '' = '';

  private readonly passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;

  constructor(private readonly fb: FormBuilder) {
    this.changePasswordForm = this.fb.group(
      {
        currentPassword: [
          '',
          [Validators.required]
        ],
        newPassword: [
          '',
          [
            Validators.required,
            Validators.pattern(this.passwordPattern)
          ]
        ],
        confirmPassword: [
          '',
          [Validators.required]
        ]
      },
      {
        validators: this.passwordMatchValidator
      }
    );

    this.newPassword?.valueChanges.subscribe((password) => {
      this.checkPasswordStrength(password ?? '');
    });
  }

  passwordMatchValidator(
    control: AbstractControl
  ): ValidationErrors | null {
    const newPassword = control.get('newPassword')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (!newPassword || !confirmPassword) {
      return null;
    }

    return newPassword === confirmPassword
      ? null
      : { passwordMismatch: true };
  }

  checkPasswordStrength(password: string): void {
    if (!password) {
      this.passwordStrength = '';
      return;
    }

    const meetsMinimumRequirements =
      this.passwordPattern.test(password);

    if (!meetsMinimumRequirements) {
      this.passwordStrength = 'low';
      return;
    }

    let additionalStrength = 0;

    if (password.length >= 12) {
      additionalStrength++;
    }

    if ((password.match(/\d/g) ?? []).length >= 2) {
      additionalStrength++;
    }

    if ((password.match(/[@$!%*?&]/g) ?? []).length >= 2) {
      additionalStrength++;
    }

    if (additionalStrength >= 2) {
      this.passwordStrength = 'strong';
    } else {
      this.passwordStrength = 'medium';
    }
  }

  hasLowercase(): boolean {
    return /[a-z]/.test(this.newPassword?.value ?? '');
  }

  hasUppercase(): boolean {
    return /[A-Z]/.test(this.newPassword?.value ?? '');
  }

  hasNumber(): boolean {
    return /\d/.test(this.newPassword?.value ?? '');
  }

  hasSpecialCharacter(): boolean {
    return /[@$!%*?&]/.test(this.newPassword?.value ?? '');
  }

  hasMinimumLength(): boolean {
    return (this.newPassword?.value ?? '').length >= 8;
  }

  get currentPassword() {
    return this.changePasswordForm.get('currentPassword');
  }

  get newPassword() {
    return this.changePasswordForm.get('newPassword');
  }

  get confirmPassword() {
    return this.changePasswordForm.get('confirmPassword');
  }

  onSubmit(): void {
    if (this.changePasswordForm.invalid) {
      this.changePasswordForm.markAllAsTouched();
      return;
    }

    // TODO: Connect to backend Change Password API when authentication service method is implemented.
    // Notice: Never log or expose password values in the console for security.
  }
}
