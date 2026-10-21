import { ChangeDetectorRef, Component, inject } from '@angular/core';
import {  FormBuilder,  FormGroup,  ReactiveFormsModule, Validators } from '@angular/forms';
import {RouterLink, Router} from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import {finalize} from 'rxjs';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})

export class Login {

  private cdr = inject(ChangeDetectorRef);
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loginForm: FormGroup;
  showPassword = false;
  isLoading = false;
  loginError = '';

  constructor() {

    this.loginForm = this.fb.group({

      email: ['', [ Validators.required,  Validators.email]],
      password: [  '',  Validators.required ]

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

    this.loginError = '';

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
  
    this.isLoading = true;

    this.authService.login(this.loginForm.value)
    .pipe(
      finalize( () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      })
    )
    .subscribe({
      next: (response) => {
        console.log('Login successful');
        console.log('Employee Id: ', response.employeeId);
        console.log('Role: ', response.role);

        if (response.role === 'EMPLOYEE') {
          this.router.navigate(['/dashboard/employee']);
        } else {
          this.loginError = 'This dashboard is for support agents only.';
        }
      },

      error: (error) => {
        console.log('Login error:', error.status);

        if(error.status === 401 || error.status === 403){
          this.loginError = 'Invalid email or pasword.';
        }
        else if(error.status === 0){
          this.loginError = 'Unable to connect to the server. Please try again.';
        }
        else{
          this.loginError = 'Login failed. Please try again later.';
        }
        this.cdr.detectChanges();
        console.error('Login request failed: ', error);
      }
    });
  }

}