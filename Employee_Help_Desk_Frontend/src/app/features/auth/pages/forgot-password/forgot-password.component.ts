import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PasswordResetFlowService } from '../../services/password-reset-flow.service';

@Component({
  selector: 'app-forgot-password',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  private readonly router = inject(Router);
  private readonly flowService = inject(PasswordResetFlowService);
  private readonly fb = inject(FormBuilder);

  readonly forgotPasswordForm: FormGroup = this.fb.group({
    email: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.pattern(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/)
      ]
    ]
  });

  readonly isSubmitted = signal(false);
  readonly isLoading = signal(false);
  readonly isSubmittedSuccess = signal(false);
  readonly submittedEmail = signal('');

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

    // Initialize mock verification session with 3 attempts in the flow service
    this.flowService.startResetSession(emailValue);
    this.submittedEmail.set(emailValue);

    // Simulate OTP dispatch and navigate to OTP verification page
    setTimeout(() => {
      this.isLoading.set(false);
      this.isSubmittedSuccess.set(true);

      setTimeout(() => {
        this.router.navigate(['/verify-otp']);
      }, 500);
    }, 400);
  }

  onResetForm(): void {
    this.isSubmittedSuccess.set(false);
    this.isSubmitted.set(false);
    this.forgotPasswordForm.reset();
  }
}

