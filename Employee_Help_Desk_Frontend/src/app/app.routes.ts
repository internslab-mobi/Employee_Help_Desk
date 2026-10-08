import { Routes } from '@angular/router';
import { ResetPassword } from './features/auth/pages/reset-password/reset-password';
import { Login } from './features/auth/pages/login/login';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: '**', redirectTo: 'login' },
  { path : 'reset-password', component : ResetPassword}
];
