import { Routes } from '@angular/router';
import { ResetPassword } from './features/auth/pages/reset-password/reset-password';
import { ForgotPasswordComponent } from './features/auth/pages/forgot-password/forgot-password.component';
import { VerifyOtp } from './features/auth/pages/verify-otp/verify-otp';
import { Login } from './features/auth/pages/login/login';
import { resetPasswordGuard, verifyOtpGuard } from './features/auth/guards/auth-flow.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'verify-otp', component: VerifyOtp, canActivate: [verifyOtpGuard] },
  { path: 'reset-password', component: ResetPassword, canActivate: [resetPasswordGuard] },
  { path: '**', redirectTo: 'login' }
];

