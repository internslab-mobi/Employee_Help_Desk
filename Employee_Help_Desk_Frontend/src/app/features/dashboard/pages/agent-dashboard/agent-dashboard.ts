import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AnalyticsService } from '../../../../core/services/analytics.service';
import { TicketService } from '../../../../core/services/ticket.service';
import { DashboardAnalyticsResponse, RecentTicket } from '../../../../core/models/dashboard.models';
import { TicketResponse } from '../../../../core/models/ticket.models';
import { DashboardStateService } from '../../services/dashboard-state.service';
import { AnalyticsDashboardView } from '../../dashboard';
import { DashboardSidebarComponent } from '../../components/sidebar/dashboard-sidebar';
import { DashboardHeaderComponent } from '../../components/header/dashboard-header';
import { NotificationDrawerComponent } from '../../components/notification-drawer/notification-drawer';
import { TicketPreviewComponent } from '../../components/ticket-preview/ticket-preview';
import { NewTicketModalComponent } from '../../components/new-ticket-modal/new-ticket-modal';
import { ReportSummaryModalComponent } from '../../components/report-summary-modal/report-summary-modal';
import { HoldModalComponent } from '../../components/hold-modal/hold-modal';
import { ResolveModalComponent } from '../../components/resolve-modal/resolve-modal';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DashboardSidebarComponent,
    DashboardHeaderComponent,
    NotificationDrawerComponent,
    TicketPreviewComponent,
    NewTicketModalComponent,
    ReportSummaryModalComponent,
    HoldModalComponent,
    ResolveModalComponent
  ],
  templateUrl: './agent-dashboard.html',
  styleUrls: ['./agent-dashboard.css']
})
export class AgentDashboard implements OnInit {
  readonly state = inject(DashboardStateService);
  private readonly analyticsService = inject(AnalyticsService);
  private readonly ticketService = inject(TicketService);
  private readonly router = inject(Router);

  // Backward compatibility fields for legacy specs
  totalAssigned = 0;
  inProgress = 0;
  slaAtRisk = 0;
  resolvedToday = 0;
  agentName = 'Support Agent';
  tickets: RecentTicket[] = [];
  searchTerm = '';
  statusFilter = '';
  priorityFilter = '';

  activeNav = signal<string>('dashboard');
  selectedAnalyticsView = signal<AnalyticsDashboardView>('OVERALL_OVERVIEW');

  analytics = signal<DashboardAnalyticsResponse | null>(null);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);

  // My Tickets (Created by Agent)
  serverTickets = signal<TicketResponse[]>([]);
  isLoadingTickets = signal<boolean>(false);
  ticketCurrentPage = signal<number>(0);
  ticketPageSize = signal<number>(10);
  ticketTotalPages = signal<number>(0);
  ticketTotalElements = signal<number>(0);
  ticketSearchTerm = signal<string>('');
  ticketStatusFilter = signal<string>('ALL');
  ticketPriorityFilter = signal<string>('ALL');

  // Assigned Tickets
  assignedTickets = signal<TicketResponse[]>([]);
  isLoadingAssignedTickets = signal<boolean>(false);
  assignedCurrentPage = signal<number>(0);
  assignedPageSize = signal<number>(10);
  assignedTotalPages = signal<number>(0);
  assignedTotalElements = signal<number>(0);
  assignedSearchTerm = signal<string>('');
  assignedStatusFilter = signal<string>('ALL');
  assignedPriorityFilter = signal<string>('ALL');

  // Filters & Toggles
  selectedDateRange = signal<string>('all');
  customFromDate = signal<string>('');
  customToDate = signal<string>('');
  showFilters = signal<boolean>(false);
  showAnalyticsSection = signal<boolean>(true);

  readonly displayTickets = computed<TicketResponse[]>(() => this.serverTickets());

  ngOnInit(): void {
    const user = this.state.currentUser();
    if (user?.firstName) {
      this.agentName = `${user.firstName} ${user.lastName || ''}`.trim();
    }
    this.state.loadUnreadCount();
    this.loadAllData();
  }

  loadAllData(): void {
    this.loadDashboardData();
    this.loadTicketsPage(0);
    this.loadAssignedTickets(0);
  }

  loadAgentData(): void {
    this.loadAllData();
  }

  get filteredTickets(): RecentTicket[] {
    const term = (this.searchTerm || this.ticketSearchTerm()).toLowerCase().trim();
    const status = (this.statusFilter || this.ticketStatusFilter()).toUpperCase().trim();
    const priority = (this.priorityFilter || this.ticketPriorityFilter()).toUpperCase().trim();

    return this.tickets.filter((t) => {
      const matchesSearch =
        !term ||
        t.ticketNumber?.toLowerCase().includes(term) ||
        t.subject?.toLowerCase().includes(term) ||
        (t.requesterName && t.requesterName.toLowerCase().includes(term));

      const matchesStatus =
        !status ||
        status === 'ALL' ||
        t.status?.toUpperCase() === status ||
        (status === 'NEW' && t.status === 'OPEN') ||
        (status === 'WAITING_FOR_EMPLOYEE' && (t.status === 'PENDING' || t.status === 'ON_HOLD'));

      const matchesPriority =
        !priority || priority === 'ALL' || t.priority?.toUpperCase() === priority;

      return matchesSearch && matchesStatus && matchesPriority;
    });
  }

  getInitials(name?: string): string {
    return this.state.getInitials(name || this.agentName);
  }

  navigateToProfile(): void {
    this.state.navigateToProfile();
  }

  setActiveNav(nav: string): void {
    this.state.clearSelectedTicket();
    if (nav === 'tickets') {
      this.activeNav.set('my-tickets');
    } else {
      this.activeNav.set(nav);
    }
  }

  setAnalyticsView(view: AnalyticsDashboardView): void {
    this.selectedAnalyticsView.set(view);
  }

  toggleAnalyticsSection(): void {
    this.showAnalyticsSection.update((v) => !v);
  }

  toggleFilters(): void {
    this.showFilters.update((v) => !v);
  }

  setDateRange(range: string): void {
    this.selectedDateRange.set(range);
    if (range !== 'custom') {
      this.loadDashboardData();
      this.loadTicketsPage(0);
      this.loadAssignedTickets(0);
    }
  }

  applyCustomDateRange(): void {
    if (this.customFromDate() && this.customToDate()) {
      if (this.customFromDate() > this.customToDate()) {
        this.state.showNotification('error', 'Start date cannot be after end date.');
        return;
      }
      this.loadDashboardData();
      this.loadTicketsPage(0);
      this.loadAssignedTickets(0);
    }
  }

  resolveDateRange(): { from?: string; to?: string } {
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

  loadDashboardData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    const { from, to } = this.resolveDateRange();

    this.analyticsService.getDashboardAnalytics(from, to).subscribe({
      next: (res: DashboardAnalyticsResponse) => {
        this.analytics.set(res);
        this.isLoading.set(false);
        if (res.summary) {
          this.totalAssigned = res.summary.assignedTickets ?? 0;
          this.inProgress = res.summary.inProgressTickets ?? 0;
          this.slaAtRisk = (res.summary.slaWarningTickets ?? 0) + (res.summary.slaBreachedTickets ?? 0);
          this.resolvedToday = res.summary.resolvedTickets ?? 0;
        }
        if (res.employeeName && !this.state.currentUser()?.firstName) {
          this.agentName = res.employeeName;
        }
        this.tickets = res.recentTickets || [];
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message || 'Failed to load dashboard data.');
      }
    });
  }

  loadTicketsPage(page = 0): void {
    this.isLoadingTickets.set(true);
    this.ticketCurrentPage.set(page);
    const { from, to } = this.resolveDateRange();

    this.ticketService
      .searchTickets({
        view: 'CREATED',
        search: this.ticketSearchTerm().trim() || undefined,
        status: this.ticketStatusFilter() !== 'ALL' ? this.ticketStatusFilter() : undefined,
        priority: this.ticketPriorityFilter() !== 'ALL' ? this.ticketPriorityFilter() : undefined,
        fromDate: from,
        toDate: to,
        page: page,
        size: this.ticketPageSize()
      })
      .subscribe({
        next: (res) => {
          this.isLoadingTickets.set(false);
          if (res && res.content) {
            this.serverTickets.set(res.content);
            this.ticketTotalElements.set(res.totalElements ?? res.content.length);
            this.ticketTotalPages.set(res.totalPages ?? 1);
          } else {
            this.serverTickets.set([]);
            this.ticketTotalElements.set(0);
            this.ticketTotalPages.set(0);
          }
        },
        error: () => {
          this.isLoadingTickets.set(false);
          this.serverTickets.set([]);
        }
      });
  }

  loadAssignedTickets(page = 0): void {
    this.isLoadingAssignedTickets.set(true);
    this.assignedCurrentPage.set(page);
    const { from, to } = this.resolveDateRange();

    this.ticketService
      .searchTickets({
        view: 'ASSIGNED',
        search: this.assignedSearchTerm().trim() || undefined,
        status: this.assignedStatusFilter() !== 'ALL' ? this.assignedStatusFilter() : undefined,
        priority: this.assignedPriorityFilter() !== 'ALL' ? this.assignedPriorityFilter() : undefined,
        fromDate: from,
        toDate: to,
        page: page,
        size: this.assignedPageSize()
      })
      .subscribe({
        next: (res) => {
          this.isLoadingAssignedTickets.set(false);
          if (res && res.content) {
            this.assignedTickets.set(res.content);
            this.assignedTotalElements.set(res.totalElements ?? res.content.length);
            this.assignedTotalPages.set(res.totalPages ?? 1);
          } else {
            this.assignedTickets.set([]);
            this.assignedTotalElements.set(0);
            this.assignedTotalPages.set(0);
          }
        },
        error: () => {
          this.isLoadingAssignedTickets.set(false);
          this.assignedTickets.set([]);
        }
      });
  }

  onAssignedSearchInput(term: string): void {
    this.assignedSearchTerm.set(term);
    this.loadAssignedTickets(0);
  }

  onAssignedStatusFilter(status: string): void {
    this.assignedStatusFilter.set(status);
    this.loadAssignedTickets(0);
  }

  onAssignedPriorityFilter(priority: string): void {
    this.assignedPriorityFilter.set(priority);
    this.loadAssignedTickets(0);
  }

  prevAssignedPage(): void {
    if (this.assignedCurrentPage() > 0) {
      this.loadAssignedTickets(this.assignedCurrentPage() - 1);
    }
  }

  nextAssignedPage(): void {
    if (this.assignedCurrentPage() + 1 < this.assignedTotalPages()) {
      this.loadAssignedTickets(this.assignedCurrentPage() + 1);
    }
  }

  onSearchInput(term: string): void {
    this.ticketSearchTerm.set(term);
    this.loadTicketsPage(0);
  }

  clearSearch(): void {
    this.ticketSearchTerm.set('');
    this.loadTicketsPage(0);
  }

  onStatusFilterChange(status: string): void {
    this.ticketStatusFilter.set(status);
    this.loadTicketsPage(0);
  }

  onPriorityFilterChange(priority: string): void {
    this.ticketPriorityFilter.set(priority);
    this.loadTicketsPage(0);
  }

  clearAllFilters(): void {
    this.ticketSearchTerm.set('');
    this.ticketStatusFilter.set('ALL');
    this.ticketPriorityFilter.set('ALL');
    this.selectedDateRange.set('all');
    this.customFromDate.set('');
    this.customToDate.set('');
    this.loadDashboardData();
    this.loadTicketsPage(0);
  }

  nextTicketPage(): void {
    if (this.ticketCurrentPage() + 1 < this.ticketTotalPages()) {
      this.loadTicketsPage(this.ticketCurrentPage() + 1);
    }
  }

  prevTicketPage(): void {
    if (this.ticketCurrentPage() > 0) {
      this.loadTicketsPage(this.ticketCurrentPage() - 1);
    }
  }

  navigateToTicketsWithFilter(filter: { status?: string }): void {
    this.state.clearSelectedTicket();
    this.activeNav.set('my-tickets');
    if (filter.status) {
      this.ticketStatusFilter.set(filter.status);
    }
    this.loadTicketsPage(0);
  }

  selectTicket(ticket: RecentTicket | TicketResponse): void {
    this.state.selectTicket(ticket);
  }

  // Agent Actions
  startWorkingTicket(ticket: TicketResponse): void {
    this.state.startWorkingTicket(ticket, () => {
      this.loadAssignedTickets(this.assignedCurrentPage());
      this.loadDashboardData();
    });
  }

  openHoldModal(ticket: TicketResponse): void {
    this.state.openHoldModal(ticket);
  }

  resumeTicketAction(ticket: TicketResponse): void {
    this.state.resumeTicketAction(ticket, () => {
      this.loadAssignedTickets(this.assignedCurrentPage());
      this.loadDashboardData();
    });
  }

  openResolveModal(ticket: TicketResponse): void {
    this.state.openResolveModal(ticket);
  }
}
