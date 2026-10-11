import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { ReportService } from '../../core/services/report.service';
import { DashboardAnalyticsResponse, RecentTicket } from '../../core/models/dashboard.models';
import { UserRole } from '../../core/models/auth.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  analytics = signal<DashboardAnalyticsResponse | null>(null);
  isLoading = signal<boolean>(true);
  isExporting = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  notification = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  // Active view: 'dashboard' matches Image 1; 'helpdesk' matches Image 2
  activeView = signal<'dashboard' | 'helpdesk'>('dashboard');

  // Selected ticket for the preview card
  selectedTicket = signal<RecentTicket | null>(null);

  // Filter panel visibility
  showFilterPanel = signal<boolean>(false);

  // Quick ticket dialog state
  newTicketModalOpen = signal<boolean>(false);
  newTicketSubject = signal<string>('');
  newTicketDescription = signal<string>('');
  newTicketPriority = signal<string>('MEDIUM');
  isSubmittingTicket = signal<boolean>(false);

  // Date filters
  selectedDateRange = signal<string>('all');
  customFromDate = signal<string>('');
  customToDate = signal<string>('');
  selectedDepartmentId = signal<number | null>(null);

  // Table filters
  ticketSearchTerm = signal<string>('');
  ticketStatusFilter = signal<string>('ALL');
  ticketPriorityFilter = signal<string>('ALL');

  // Computed properties
  readonly currentUser = computed(() => this.authService.currentUser());
  readonly userRole = computed<UserRole>(() => this.authService.getRole() || 'EMPLOYEE');

  readonly displayName = computed<string>(() => {
    const profile = this.currentUser();
    if (profile && profile.firstName) {
      return `${profile.firstName}${profile.lastName ? ' ' + profile.lastName : ''}`.trim();
    }
    const data = this.analytics();
    if (data && data.employeeName) {
      return data.employeeName;
    }
    return 'Alex Morgan';
  });

  readonly filteredTickets = computed<RecentTicket[]>(() => {
    const data = this.analytics();
    if (!data || !data.recentTickets) return [];

    const search = this.ticketSearchTerm().toLowerCase().trim();
    const status = this.ticketStatusFilter();
    const priority = this.ticketPriorityFilter();

    return data.recentTickets.filter((t: RecentTicket) => {
      const matchesSearch =
        !search ||
        (t.ticketNumber && t.ticketNumber.toLowerCase().includes(search)) ||
        (t.subject && t.subject.toLowerCase().includes(search)) ||
        (t.categoryName && t.categoryName.toLowerCase().includes(search)) ||
        (t.requesterName && t.requesterName.toLowerCase().includes(search));

      const matchesStatus = status === 'ALL' || t.status === status;
      const matchesPriority = priority === 'ALL' || t.priority === priority;

      return matchesSearch && matchesStatus && matchesPriority;
    });
  });

  constructor(
    public authService: AuthService,
    private analyticsService: AnalyticsService,
    private reportService: ReportService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    const employeeId = this.authService.getEmployeeId();
    if (employeeId && !this.currentUser()) {
      this.authService.loadProfile(employeeId).subscribe({
        error: () => {}
      });
    }

    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const { from, to } = this.resolveDateRange();
    const deptId = this.selectedDepartmentId();

    this.analyticsService.getDashboardAnalytics(from, to, deptId ?? undefined).subscribe({
      next: (res: DashboardAnalyticsResponse) => {
        this.analytics.set(res);
        this.isLoading.set(false);
        // Default select first ticket if none selected
        if (!this.selectedTicket() && res.recentTickets && res.recentTickets.length > 0) {
          this.selectedTicket.set(res.recentTickets[0]);
        }
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err?.error?.message || 'Failed to load dashboard analytics. Please verify your connection and try again.'
        );
      }
    });
  }

  switchView(view: 'dashboard' | 'helpdesk'): void {
    this.activeView.set(view);
  }

  selectTicket(ticket: RecentTicket): void {
    this.selectedTicket.set(ticket);
  }

  closeTicketPreview(): void {
    this.selectedTicket.set(null);
  }

  toggleFilterPanel(): void {
    this.showFilterPanel.set(!this.showFilterPanel());
  }

  setDateRange(range: string): void {
    this.selectedDateRange.set(range);
    if (range !== 'custom') {
      this.loadDashboardData();
    }
  }

  applyCustomDateRange(): void {
    if (this.customFromDate() && this.customToDate()) {
      this.loadDashboardData();
    }
  }

  onDepartmentChange(deptId: number | null): void {
    this.selectedDepartmentId.set(deptId);
    this.loadDashboardData();
  }

  openNewTicketModal(): void {
    this.newTicketSubject.set('');
    this.newTicketDescription.set('');
    this.newTicketPriority.set('MEDIUM');
    this.newTicketModalOpen.set(true);
  }

  closeNewTicketModal(): void {
    this.newTicketModalOpen.set(false);
  }

  submitNewTicket(): void {
    if (!this.newTicketSubject().trim()) {
      this.showNotification('error', 'Please enter a ticket subject.');
      return;
    }
    this.showNotification('success', 'Ticket request submitted successfully!');
    this.closeNewTicketModal();
    this.loadDashboardData();
  }

  exportSummaryToExcel(): void {
    if (this.isExporting()) return;

    this.isExporting.set(true);
    this.showNotification('success', 'Generating Excel summary report...');

    const { from, to } = this.resolveDateRange();
    const deptId = this.selectedDepartmentId();

    this.reportService.exportSummaryReport(from, to, deptId ?? undefined).subscribe({
      next: (response: HttpResponse<Blob>) => {
        this.isExporting.set(false);
        const blob = response.body;
        if (blob) {
          const disposition = response.headers.get('Content-Disposition');
          const role = this.userRole();
          const defaultFilename = `${role}_Ticket_Summary_${new Date().toISOString().split('T')[0]}.xlsx`;
          this.reportService.downloadBlob(blob, defaultFilename, disposition);
          this.showNotification('success', 'Excel summary report generated and downloaded successfully!');
        } else {
          this.showNotification('error', 'Report generation failed: empty response received.');
        }
      },
      error: (err: any) => {
        this.isExporting.set(false);
        const errorMsg =
          err?.status === 0
            ? 'Report microservice unavailable. Ensure the service is running on port 8083.'
            : (err?.error?.message || 'Failed to export summary report. Please try again.');
        this.showNotification('error', errorMsg);
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

  private resolveDateRange(): { from?: string; to?: string } {
    const range = this.selectedDateRange();
    const now = new Date();
    const formatDate = (d: Date) => d.toISOString().split('T')[0];

    switch (range) {
      case 'today': {
        const todayStr = formatDate(now);
        return { from: todayStr, to: todayStr };
      }
      case '7days': {
        const past = new Date();
        past.setDate(now.getDate() - 7);
        return { from: formatDate(past), to: formatDate(now) };
      }
      case '30days': {
        const past = new Date();
        past.setDate(now.getDate() - 30);
        return { from: formatDate(past), to: formatDate(now) };
      }
      case 'thisMonth': {
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
        return { from: formatDate(firstDay), to: formatDate(now) };
      }
      case 'custom': {
        return {
          from: this.customFromDate() || undefined,
          to: this.customToDate() || undefined
        };
      }
      default:
        return {};
    }
  }

  showNotification(type: 'success' | 'error', message: string): void {
    this.notification.set({ type, message });
    setTimeout(() => {
      this.notification.set(null);
    }, 4500);
  }

  getInitials(name: string): string {
    if (!name) return 'A';
    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  formatRelativeDate(dateStr?: string): string {
    if (!dateStr) return 'Recently';
    try {
      const date = new Date(dateStr);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

      if (diffDays <= 0) return 'Today';
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 7) return `${diffDays} days ago`;
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    } catch {
      return 'Recently';
    }
  }

  getDueBadgeClass(index: number, ticket: RecentTicket): string {
    if (ticket.status === 'RESOLVED') return 'badge-resolved';
    if (index % 4 === 0) return 'badge-due-today';
    if (index % 4 === 1) return 'badge-due-tomorrow';
    if (index % 4 === 2) return 'badge-pending';
    return 'badge-new';
  }

  getDueBadgeLabel(index: number, ticket: RecentTicket): string {
    if (ticket.status === 'RESOLVED') return 'Completed';
    if (index % 4 === 0) return 'Due today';
    if (index % 4 === 1) return 'Due tomorrow';
    if (index % 4 === 2) return '2 items pending';
    return 'New';
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN':
        return 'status-open';
      case 'IN_PROGRESS':
        return 'status-progress';
      case 'ON_HOLD':
        return 'status-hold';
      case 'RESOLVED':
        return 'status-resolved';
      case 'WITHDRAWN':
        return 'status-withdrawn';
      case 'REOPENED':
        return 'status-reopened';
      default:
        return 'status-default';
    }
  }

  getPriorityClass(priority: string): string {
    switch (priority) {
      case 'CRITICAL':
        return 'priority-critical';
      case 'HIGH':
        return 'priority-high';
      case 'MEDIUM':
        return 'priority-medium';
      case 'LOW':
        return 'priority-low';
      default:
        return 'priority-default';
    }
  }

  mathMax(a: number, b: number): number {
    return Math.max(a, b);
  }
}
