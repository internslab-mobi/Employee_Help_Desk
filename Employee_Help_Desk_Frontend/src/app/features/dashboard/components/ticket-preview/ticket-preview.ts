import { Component, input, output, inject, signal, effect } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardStateService } from '../../services/dashboard-state.service';
import { TicketService } from '../../../../core/services/ticket.service';

@Component({
  selector: 'app-ticket-preview',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, DecimalPipe],
  templateUrl: './ticket-preview.html',
  styleUrls: ['./ticket-preview.css']
})
export class TicketPreviewComponent {
  readonly state = inject(DashboardStateService);
  private readonly ticketService = inject(TicketService);

  activeNav = input<string>('');
  actionCompleted = output<void>();

  showWithdrawConfirm = signal<boolean>(false);
  withdrawReason = signal<string>('');

  constructor() {
    let prevNav = '';
    effect(() => {
      const current = this.activeNav();
      // Whenever switching between sections (e.g. my-tickets <-> assigned-tickets), clear previous ticket preview
      if (prevNav && current !== prevNav) {
        this.state.clearSelectedTicket();
        this.closeWithdrawConfirmation();
      }
      prevNav = current;
    });
  }

  canShowAgentActions(): boolean {
    // 1. Only Agents can perform agent actions (Admin doesn't handle tickets, Manager assigns)
    if (this.state.userRole() !== 'AGENT') {
      return false;
    }

    // 2. Under 'my-tickets' tab, the user is viewing tickets they raised -> never show agent actions
    if (this.activeNav() === 'my-tickets') {
      return false;
    }

    const ticket = this.state.selectedTicketForPreview();
    if (!ticket) {
      return false;
    }

    // 3. If this ticket was raised by the current user, do not show agent actions
    if (this.state.isTicketRaisedByCurrentUser(ticket)) {
      return false;
    }

    // 4. Must be assigned to this agent:
    if (this.activeNav() === 'assigned-tickets') {
      return true;
    }

    return this.state.isTicketAssignedToCurrentUser(ticket);
  }

  canWithdrawTicket(): boolean {
    const ticket = this.state.selectedTicketForPreview();
    if (!ticket) return false;

    // 1. Must be in 'my-tickets' section or ticket raised by current user
    const isMySection = this.activeNav() === 'my-tickets' || this.activeNav() === 'helpdesk';
    const isRaisedBySelf = this.state.isTicketRaisedByCurrentUser(ticket);
    if (!isMySection && !isRaisedBySelf) {
      return false;
    }

    // 2. Cannot withdraw if already RESOLVED or WITHDRAWN
    const status = (ticket.status || '').toUpperCase();
    if (status === 'WITHDRAWN' || status === 'RESOLVED') {
      return false;
    }

    return true;
  }

  getReopenCount(): number {
    const full = this.state.fullSelectedTicket();
    const selected = this.state.selectedTicketForPreview();
    return full?.reopenCount ?? selected?.reopenCount ?? 0;
  }

  canReopenTicket(): boolean {
    const ticket = this.state.selectedTicketForPreview();
    if (!ticket) return false;

    // 1. Must be in RESOLVED status
    const status = (ticket.status || '').toUpperCase();
    if (status !== 'RESOLVED') {
      return false;
    }

    // 2. Can only be reopened at most twice (reopenCount < 2)
    const count = this.getReopenCount();
    if (count >= 2) {
      return false;
    }

    // 3. Must be raised by current user or viewed under 'my-tickets'
    const isMySection = this.activeNav() === 'my-tickets' || this.activeNav() === 'helpdesk';
    const isRaisedBySelf = this.state.isTicketRaisedByCurrentUser(ticket);

    return isMySection || isRaisedBySelf;
  }

  openWithdrawConfirmation(): void {
    if (!this.state.checkBusinessHours('withdraw')) {
      return;
    }
    this.withdrawReason.set('');
    this.showWithdrawConfirm.set(true);
  }

  closeWithdrawConfirmation(): void {
    this.showWithdrawConfirm.set(false);
    this.withdrawReason.set('');
  }

  confirmWithdraw(): void {
    const target = this.state.selectedTicketForPreview();
    if (!target) return;

    if (!this.state.checkBusinessHours('withdraw')) {
      return;
    }

    const reason = this.withdrawReason().trim() || 'Withdrawn by requester';
    this.state.isSubmittingAction.set(true);

    this.ticketService.updateTicket(target.id, { status: 'WITHDRAWN', reason }).subscribe({
      next: (updated: any) => {
        this.state.isSubmittingAction.set(false);
        this.closeWithdrawConfirmation();
        this.state.showNotification('success', `Ticket ${target.ticketNumber} has been withdrawn.`);
        this.state.selectTicket(updated || target);
        this.actionCompleted.emit();
      },
      error: (err: any) => {
        this.state.isSubmittingAction.set(false);
        this.state.showNotification('error', err?.error?.message || 'Failed to withdraw ticket.');
      }
    });
  }

  showReopenConfirm = signal<boolean>(false);
  reopenReason = signal<string>('');

  openReopenConfirmation(): void {
    this.reopenReason.set('');
    this.showReopenConfirm.set(true);
  }

  closeReopenConfirmation(): void {
    this.showReopenConfirm.set(false);
    this.reopenReason.set('');
  }

  confirmReopen(): void {
    const target = this.state.selectedTicketForPreview();
    if (!target) return;

    const reason = this.reopenReason().trim() || 'Issue persists / needs further investigation';
    this.state.isSubmittingAction.set(true);

    this.ticketService.updateTicket(target.id, { status: 'REOPEN', reason }).subscribe({
      next: (updated: any) => {
        this.state.isSubmittingAction.set(false);
        this.closeReopenConfirmation();
        this.state.showNotification('success', `Ticket ${target.ticketNumber} reopened successfully. SLA restarted with 50% duration.`);
        this.state.selectTicket(updated || target);
        this.actionCompleted.emit();
      },
      error: (err: any) => {
        this.state.isSubmittingAction.set(false);
        this.state.showNotification('error', err?.error?.message || 'Failed to reopen ticket.');
      }
    });
  }

  onStartWorking(): void {
    this.state.startWorkingTicket(this.state.selectedTicketForPreview(), () => {
      this.actionCompleted.emit();
    });
  }

  onOpenHold(): void {
    this.state.openHoldModal(this.state.selectedTicketForPreview());
  }

  onResume(): void {
    this.state.resumeTicketAction(this.state.selectedTicketForPreview(), () => {
      this.actionCompleted.emit();
    });
  }

  onOpenResolve(): void {
    this.state.openResolveModal(this.state.selectedTicketForPreview());
  }
}
