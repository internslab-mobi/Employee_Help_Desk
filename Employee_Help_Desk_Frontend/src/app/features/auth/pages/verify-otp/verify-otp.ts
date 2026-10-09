import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  computed,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { PasswordResetFlowService } from '../../services/password-reset-flow.service';

@Component({
  selector: 'app-verify-otp',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './verify-otp.html',
  styleUrl: './verify-otp.css'
})
export class VerifyOtp implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  readonly flowService = inject(PasswordResetFlowService);

  readonly otpDigits = signal<string[]>(['', '', '', '', '', '']);
  readonly isVerifying = signal<boolean>(false);
  readonly errorMessage = signal<string>('');
  readonly successMessage = signal<string>('');
  readonly countdown = signal<number>(30);

  private countdownInterval: any = null;

  readonly remainingAttempts = this.flowService.remainingAttempts;
  readonly maskedEmail = this.flowService.maskedEmail;
  readonly isLocked = computed(() => this.remainingAttempts() <= 0);

  readonly isComplete = computed(() => {
    return this.otpDigits().every(digit => digit.trim().length === 1 && /^\d$/.test(digit));
  });

  readonly canResend = computed(() => {
    return this.countdown() === 0 && !this.isVerifying();
  });

  ngOnInit(): void {
    // If no active reset session exists, redirect to forgot password
    if (!this.flowService.canAccessVerifyOtp()) {
      this.router.navigate(['/forgot-password']);
      return;
    }

    this.startCountdown();

    // Auto-focus the first OTP input
    setTimeout(() => {
      this.focusInput(0);
    }, 100);
  }

  ngOnDestroy(): void {
    this.stopCountdown();
  }

  startCountdown(): void {
    this.stopCountdown();
    this.countdown.set(30);

    this.countdownInterval = setInterval(() => {
      const current = this.countdown();
      if (current > 1) {
        this.countdown.set(current - 1);
      } else {
        this.countdown.set(0);
        this.stopCountdown();
      }
    }, 1000);
  }

  stopCountdown(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
      this.countdownInterval = null;
    }
  }

  focusInput(index: number): void {
    if (index >= 0 && index < 6) {
      const el = document.getElementById(`otp-input-${index}`) as HTMLInputElement | null;
      if (el) {
        el.focus();
        el.select();
      }
    }
  }

  onInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const val = input.value;

    if (!val) {
      const digits = [...this.otpDigits()];
      digits[index] = '';
      this.otpDigits.set(digits);
      return;
    }

    // Only allow single digit
    const lastChar = val.slice(-1);
    if (/^\d$/.test(lastChar)) {
      const digits = [...this.otpDigits()];
      digits[index] = lastChar;
      this.otpDigits.set(digits);
      this.errorMessage.set('');

      if (index < 5) {
        this.focusInput(index + 1);
      }
    } else {
      input.value = this.otpDigits()[index];
    }
  }

  onKeyDown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace') {
      const digits = [...this.otpDigits()];
      if (digits[index]) {
        digits[index] = '';
        this.otpDigits.set(digits);
      } else if (index > 0) {
        digits[index - 1] = '';
        this.otpDigits.set(digits);
        this.focusInput(index - 1);
      }
      event.preventDefault();
      return;
    }

    if (event.key === 'ArrowLeft' && index > 0) {
      event.preventDefault();
      this.focusInput(index - 1);
      return;
    }

    if (event.key === 'ArrowRight' && index < 5) {
      event.preventDefault();
      this.focusInput(index + 1);
      return;
    }

    // Ignore tab, navigation, or control/command keys
    if (
      event.key === 'Tab' ||
      event.key === 'Delete' ||
      event.ctrlKey ||
      event.metaKey
    ) {
      return;
    }

    // Reject non-numeric keys
    if (!/^\d$/.test(event.key)) {
      event.preventDefault();
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const clipboardData = event.clipboardData?.getData('text') ?? '';
    const numericChars = clipboardData.replace(/\D/g, '').slice(0, 6);

    if (!numericChars) {
      return;
    }

    const newDigits = [...this.otpDigits()];
    for (let i = 0; i < 6; i++) {
      newDigits[i] = numericChars[i] ?? '';
    }
    this.otpDigits.set(newDigits);
    this.errorMessage.set('');

    const targetIndex = Math.min(numericChars.length, 5);
    this.focusInput(targetIndex);
  }

  onVerify(): void {
    if (this.isLocked() || this.isVerifying() || !this.isComplete()) {
      return;
    }

    this.isVerifying.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const code = this.otpDigits().join('');

    setTimeout(() => {
      const result = this.flowService.verifyOtp(code);
      this.isVerifying.set(false);

      if (result.success) {
        this.successMessage.set('OTP verified successfully!');
        setTimeout(() => {
          this.router.navigate(['/reset-password']);
        }, 600);
      } else {
        this.errorMessage.set(result.message);
        if (result.isLocked) {
          // Lock state triggered
        } else {
          // Clear inputs for convenient re-entry
          this.otpDigits.set(['', '', '', '', '', '']);
          this.focusInput(0);
        }
      }
    }, 400);
  }

  onResend(): void {
    if (!this.canResend()) {
      return;
    }

    this.flowService.resendOtp();
    this.otpDigits.set(['', '', '', '', '', '']);
    this.errorMessage.set('');
    this.successMessage.set('A new OTP has been sent to your email.');
    this.startCountdown();

    setTimeout(() => {
      this.focusInput(0);
    }, 100);
  }
}
