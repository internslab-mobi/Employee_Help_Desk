import { Injectable, signal, computed } from '@angular/core';

export interface VerifyOtpResult {
  success: boolean;
  message: string;
  remainingAttempts: number;
  isLocked: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class PasswordResetFlowService {
  /**
   * Development-only mock OTP
   */
  readonly MOCK_OTP = '123456';
  readonly MAX_ATTEMPTS = 3;

  private readonly emailSignal = signal<string>('');
  private readonly attemptsSignal = signal<number>(this.MAX_ATTEMPTS);
  private readonly isOtpVerifiedSignal = signal<boolean>(false);
  private readonly isSessionActiveSignal = signal<boolean>(false);

  readonly email = this.emailSignal.asReadonly();
  readonly remainingAttempts = this.attemptsSignal.asReadonly();
  readonly isOtpVerified = this.isOtpVerifiedSignal.asReadonly();
  readonly isSessionActive = this.isSessionActiveSignal.asReadonly();

  readonly maskedEmail = computed(() => {
    return this.maskEmail(this.emailSignal());
  });

  /**
   * Start a new password reset session with the provided email.
   * Initializes 3 verification attempts.
   */
  startResetSession(email: string): void {
    this.emailSignal.set(email.trim());
    this.attemptsSignal.set(this.MAX_ATTEMPTS);
    this.isOtpVerifiedSignal.set(false);
    this.isSessionActiveSignal.set(true);
  }

  /**
   * Verify entered OTP against the mock OTP.
   */
  verifyOtp(enteredOtp: string): VerifyOtpResult {
    const currentAttempts = this.attemptsSignal();

    if (currentAttempts <= 0) {
      return {
        success: false,
        message: 'Maximum verification attempts exceeded (3/3). Please request a new OTP.',
        remainingAttempts: 0,
        isLocked: true
      };
    }

    if (enteredOtp === this.MOCK_OTP) {
      this.isOtpVerifiedSignal.set(true);
      return {
        success: true,
        message: 'OTP verified successfully!',
        remainingAttempts: currentAttempts,
        isLocked: false
      };
    }

    const updatedAttempts = currentAttempts - 1;
    this.attemptsSignal.set(updatedAttempts);

    if (updatedAttempts === 0) {
      return {
        success: false,
        message: 'Incorrect OTP. You have exhausted all 3 attempts. Please request a new OTP.',
        remainingAttempts: 0,
        isLocked: true
      };
    }

    return {
      success: false,
      message: `Incorrect OTP. You have ${updatedAttempts} attempt${updatedAttempts === 1 ? '' : 's'} remaining.`,
      remainingAttempts: updatedAttempts,
      isLocked: false
    };
  }

  /**
   * Resend OTP: resets verification attempts to 3 and starts a fresh mock session.
   */
  resendOtp(): void {
    this.attemptsSignal.set(this.MAX_ATTEMPTS);
    this.isOtpVerifiedSignal.set(false);
  }

  /**
   * Completes the reset process and clears all in-memory temporary state.
   */
  completeReset(): void {
    this.resetSession();
  }

  /**
   * Resets all in-memory flow state.
   */
  resetSession(): void {
    this.emailSignal.set('');
    this.attemptsSignal.set(this.MAX_ATTEMPTS);
    this.isOtpVerifiedSignal.set(false);
    this.isSessionActiveSignal.set(false);
  }

  /**
   * Mask an email address (e.g., john.doe@company.com -> j***e@company.com).
   */
  maskEmail(email: string): string {
    if (!email || !email.includes('@')) {
      return email || '';
    }

    const [user, domain] = email.split('@');
    if (!user || !domain) {
      return email;
    }

    if (user.length <= 1) {
      return `${user}***@${domain}`;
    }

    if (user.length === 2) {
      return `${user[0]}***@${domain}`;
    }

    return `${user[0]}***${user[user.length - 1]}@${domain}`;
  }

  canAccessVerifyOtp(): boolean {
    return this.isSessionActiveSignal() && this.emailSignal().length > 0;
  }

  canAccessResetPassword(): boolean {
    return this.isOtpVerifiedSignal() && this.emailSignal().length > 0;
  }
}
