import { Routes } from '@angular/router';
import { ResetPassword } from './features/auth/pages/reset-password/reset-password';
import { ForgotPasswordComponent } from './features/auth/pages/forgot-password/forgot-password.component';
import { Login } from './features/auth/pages/login/login';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'forgot-password', component: ForgotPasswordComponent},
  { path : 'reset-password', component : ResetPassword},
  { path: '**', redirectTo: 'login' }
];
