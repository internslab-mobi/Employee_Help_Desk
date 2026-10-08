import { Routes } from '@angular/router';
import { ForgotPasswordComponent } from './features/auth/pages/forgot-password/forgot-password.component';

export const routes: Routes = [
  { path: '', component: ForgotPasswordComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: '**', redirectTo: '' }
];
