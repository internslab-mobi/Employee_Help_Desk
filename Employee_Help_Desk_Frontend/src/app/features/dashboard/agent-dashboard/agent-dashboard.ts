
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { AuthService } from '../../../core/services/auth.service';
import { DashboardAnalyticsResponse, RecentTicket } from '../../../core/models/dashboard.models';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './agent-dashboard.html',
  styleUrl: './agent-dashboard.css'
})
export class AgentDashboard implements OnInit {
  private readonly analyticsService = inject(AnalyticsService, { optional: true });
  private readonly authService = inject(AuthService, { optional: true });
  private readonly router = inject(Router, { optional: true });

  totalAssigned = 0;
  inProgress = 0;
  slaAtRisk = 0;
  resolvedToday = 0;

  agentName = 'Support Agent';
  tickets: RecentTicket[] = [];
  searchTerm = '';
  statusFilter = '';
  priorityFilter = '';
  isLoading = false;
  errorMessage = '';

  ngOnInit(): void {
    const user = this.authService?.currentUser();
    if (user?.firstName) {
      this.agentName = `${user.firstName} ${user.lastName || ''}`.trim();
    }
    this.loadAgentData();
  }

  loadAgentData(): void {
    if (!this.analyticsService) return;

    this.isLoading = true;
    this.errorMessage = '';

    this.analyticsService.getDashboardAnalytics().subscribe({
      next: (res: DashboardAnalyticsResponse) => {
        this.isLoading = false;
        if (res.summary) {
          this.totalAssigned = res.summary.assignedTickets ?? 0;
          this.inProgress = res.summary.inProgressTickets ?? 0;
          this.slaAtRisk = (res.summary.slaWarningTickets ?? 0) + (res.summary.slaBreachedTickets ?? 0);
          this.resolvedToday = res.summary.resolvedTickets ?? 0;
        }
        if (res.employeeName && (!this.authService?.currentUser()?.firstName)) {
          this.agentName = res.employeeName;
        }
        this.tickets = res.recentTickets || [];
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err?.error?.message || 'Failed to load assigned ticket analytics.';
      }
    });
  }

  get filteredTickets(): RecentTicket[] {
    const term = this.searchTerm.toLowerCase().trim();
    const status = this.statusFilter.toUpperCase().trim();
    const priority = this.priorityFilter.toUpperCase().trim();

    return this.tickets.filter((t) => {
      const matchesSearch =
        !term ||
        t.ticketNumber?.toLowerCase().includes(term) ||
        t.subject?.toLowerCase().includes(term) ||
        (t.requesterName && t.requesterName.toLowerCase().includes(term));

      const matchesStatus =
        !status ||
        t.status?.toUpperCase() === status ||
        (status === 'NEW' && t.status === 'OPEN') ||
        (status === 'WAITING_FOR_EMPLOYEE' && (t.status === 'PENDING' || t.status === 'ON_HOLD'));

      const matchesPriority =
        !priority || t.priority?.toUpperCase() === priority;

      return matchesSearch && matchesStatus && matchesPriority;
    });
  }

  getInitials(name?: string): string {
    if (!name) return 'A';
    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  getStatusCssClass(status?: string): string {
    switch (status?.toUpperCase()) {
      case 'OPEN':
      case 'NEW':
        return 'new';
      case 'IN_PROGRESS':
        return 'in-progress';
      case 'PENDING':
      case 'ON_HOLD':
      case 'WAITING_FOR_EMPLOYEE':
        return 'waiting';
      case 'RESOLVED':
        return 'in-progress';
      default:
        return 'new';
    }
  }

  formatStatus(status?: string): string {
    if (!status) return 'New';
    switch (status.toUpperCase()) {
      case 'OPEN':
        return 'Open';
      case 'IN_PROGRESS':
        return 'In Progress';
      case 'ON_HOLD':
      case 'PENDING':
        return 'Waiting for Employee';
      case 'RESOLVED':
        return 'Resolved';
      case 'REOPENED':
        return 'Reopened';
      default:
        return status.replace(/_/g, ' ');
    }
  }

  viewTicket(ticket: RecentTicket): void {
    if (this.router) {
      this.router.navigate(['/dashboard']);
    }
  }
}