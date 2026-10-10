import { Routes } from '@angular/router';
import { ResetPassword } from './features/auth/pages/reset-password/reset-password';
import { ForgotPasswordComponent } from './features/auth/pages/forgot-password/forgot-password.component';
import { Login } from './features/auth/pages/login/login';
import { AgentDashboard } from './features/dashboard/agent-dashboard/agent-dashboard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'forgot-password', component: ForgotPasswordComponent},
  { path : 'reset-password', component : ResetPassword},
  { path: 'dashboard/employee', component: AgentDashboard},
  { path: '**', redirectTo: 'login' }
];
