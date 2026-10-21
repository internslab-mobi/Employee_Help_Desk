import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PasswordResetFlowService } from '../services/password-reset-flow.service';

/**
 * Route guard for OTP Verification page.
 * Prevents direct URL entry or browser navigation if no active reset session exists.
 */
export const verifyOtpGuard: CanActivateFn = () => {
  const flowService = inject(PasswordResetFlowService);
  const router = inject(Router);

  if (flowService.canAccessVerifyOtp()) {
    return true;
  }

  return router.createUrlTree(['/forgot-password']);
};

/**
 * Route guard for Reset Password page.
 * Enforces that Reset Password can ONLY be accessed if OTP was successfully verified in-memory.
 * Otherwise redirects to Forgot Password.
 */
export const resetPasswordGuard: CanActivateFn = () => {
  const flowService = inject(PasswordResetFlowService);
  const router = inject(Router);

  if (flowService.canAccessResetPassword()) {
    return true;
  }

  return router.createUrlTree(['/forgot-password']);
};
