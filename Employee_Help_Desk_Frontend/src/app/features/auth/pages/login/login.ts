import { Component, ChangeDetectorRef, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  loginForm: FormGroup;
  showPassword = false;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  get email() {
    return this.loginForm.get('email');
  }

  get password() {
    return this.loginForm.get('password');
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onLogin(): void {
    if (this.isLoading) {
      return;
    }

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const credentials = {
      email: this.loginForm.value.email?.trim() || '',
      password: this.loginForm.value.password || ''
    };

    this.authService
      .login(credentials)
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          let backendMessage = '';

          if (typeof err?.error === 'string' && err.error.trim().length > 0) {
            backendMessage = err.error.trim();
          } else if (err?.error && typeof err.error === 'object') {
            backendMessage =
              err.error.message ||
              err.error.error ||
              err.error.detail ||
              '';
          }

          if (backendMessage) {
            this.errorMessage = backendMessage;
          } else if (err?.status === 0) {
            this.errorMessage = 'Unable to connect to the authentication server. Please ensure the backend is running.';
          } else {
            this.errorMessage = 'Authentication failed. Please try again.';
          }

          this.cdr.detectChanges();
        }
      });
  }
}
