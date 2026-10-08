import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  readonly forgotPasswordForm: FormGroup;
  readonly isSubmitted = signal(false);
  readonly isLoading = signal(false);
  readonly isSubmittedSuccess = signal(false);
  readonly submittedEmail = signal('');

  constructor(private readonly fb: FormBuilder) {
    this.forgotPasswordForm = this.fb.group({
      email: [
        '',
        [
          Validators.required,
          Validators.email,
          Validators.pattern(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/)
        ]
      ]
    });
  }

  get emailControl() {
    return this.forgotPasswordForm.get('email');
  }

  get emailErrorMessage(): string {
    const control = this.emailControl;
    if (!control || !control.errors || (!control.touched && !this.isSubmitted())) {
      return '';
    }

    if (control.errors['required']) {
      return 'Email address is required.';
    }

    if (control.errors['email'] || control.errors['pattern']) {
      return 'Please enter a valid work email address.';
    }

    return '';
  }

  onSubmit(): void {
    this.isSubmitted.set(true);

    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const emailValue = this.forgotPasswordForm.value.email?.trim() ?? '';

    // Self-contained simulation for UI demonstration
    setTimeout(() => {
      this.isLoading.set(false);
      this.isSubmittedSuccess.set(true);
      this.submittedEmail.set(emailValue);
    }, 600);
  }

  onResetForm(): void {
    this.isSubmittedSuccess.set(false);
    this.isSubmitted.set(false);
    this.forgotPasswordForm.reset();
  }
}
