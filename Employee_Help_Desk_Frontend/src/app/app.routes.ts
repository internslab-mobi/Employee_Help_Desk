import { Routes } from '@angular/router';
import { ResetPassword } from './features/auth/pages/reset-password/reset-password';
import { ForgotPasswordComponent } from './features/auth/pages/forgot-password/forgot-password.component';
import { VerifyOtp } from './features/auth/pages/verify-otp/verify-otp';
import { Login } from './features/auth/pages/login/login';
import { AgentDashboard } from './features/dashboard/pages/agent-dashboard/agent-dashboard';
import { ChangePassword } from './features/auth/pages/change-password/change-password';
import { DashboardComponent } from './features/dashboard/dashboard';
import { ProfileComponent } from './features/profile/profile';
import { resetPasswordGuard, verifyOtpGuard } from './features/auth/guards/auth-flow.guard';
import { authGuard } from './core/guards/auth.guard';

import { EmployeeDashboardComponent } from './features/dashboard/pages/employee-dashboard/employee-dashboard';
import { ManagerDashboardComponent } from './features/dashboard/pages/manager-dashboard/manager-dashboard';
import { AdminDashboardComponent } from './features/dashboard/pages/admin-dashboard/admin-dashboard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'tickets', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'verify-otp', component: VerifyOtp, canActivate: [verifyOtpGuard] },
  { path: 'reset-password', component: ResetPassword, canActivate: [resetPasswordGuard] },
  { path: 'change-password', component: ChangePassword },
  { path: 'dashboard/employee', component: EmployeeDashboardComponent, canActivate: [authGuard] },
  { path: 'dashboard/agent', component: AgentDashboard, canActivate: [authGuard] },
  { path: 'dashboard/manager', component: ManagerDashboardComponent, canActivate: [authGuard] },
  { path: 'dashboard/admin', component: AdminDashboardComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' }
];
