import { Routes } from '@angular/router';
import { ResetPassword } from './features/auth/pages/reset-password/reset-password';
import { ForgotPasswordComponent } from './features/auth/pages/forgot-password/forgot-password.component';

export const routes: Routes = [
    {
        path : 'reset-password',
        component : ResetPassword
    },
    {
      path : 'forgot-password',
      component : ForgotPasswordComponent
    }
];
