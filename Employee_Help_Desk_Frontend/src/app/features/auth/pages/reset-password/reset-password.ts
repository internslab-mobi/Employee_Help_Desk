import { Component } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';

import { RouterLink } from '@angular/router';
@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css'
})
export class ResetPassword {

  resetPasswordForm;

  passwordStrength: 'low' | 'medium' | 'strong' | '' = '';

  private readonly passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;

  constructor(private fb: FormBuilder) {

    this.resetPasswordForm = this.fb.group(
      {
        newPassword: [
          '',
          [
            Validators.required,
            Validators.pattern(this.passwordPattern)
          ]
        ],

        confirmPassword: [
          '',
          [
            Validators.required
          ]
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


  get newPassword() {
    return this.resetPasswordForm.get('newPassword');
  }


  get confirmPassword() {
    return this.resetPasswordForm.get('confirmPassword');
  }


  onSubmit(): void {

    if (this.resetPasswordForm.invalid) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }

    console.log('Password is valid');
    console.log(this.resetPasswordForm.value);

    // Backend API integration will be added later.
  }
}