import { Component } from '@angular/core';
import {  FormBuilder,  FormGroup,  ReactiveFormsModule, Validators } from '@angular/forms';
import {RouterLink} from '@angular/router';

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

  constructor(private fb: FormBuilder) {

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
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    /*
      Backend integration will be added later.
      For now, just check the form values.
    */
    console.log('Login:', this.loginForm.value);
  }

}