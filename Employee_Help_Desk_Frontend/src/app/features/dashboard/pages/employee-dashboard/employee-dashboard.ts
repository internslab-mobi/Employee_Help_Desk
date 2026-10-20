import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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

@Component({
  selector: 'app-employee-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DashboardSidebarComponent,
    DashboardHeaderComponent,
    NotificationDrawerComponent,
    TicketPreviewComponent,
    NewTicketModalComponent,
    ReportSummaryModalComponent
  ],
  templateUrl: './employee-dashboard.html',
  styleUrls: ['./employee-dashboard.css']
})
export class EmployeeDashboardComponent implements OnInit {
  readonly state = inject(DashboardStateService);
  private readonly analyticsService = inject(AnalyticsService);
  private readonly ticketService = inject(TicketService);

  activeNav = signal<string>('dashboard');
  selectedAnalyticsView = signal<AnalyticsDashboardView>('OVERALL_OVERVIEW');

  analytics = signal<DashboardAnalyticsResponse | null>(null);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);

  // Tickets List State
  serverTickets = signal<TicketResponse[]>([]);
  isLoadingTickets = signal<boolean>(false);
  ticketCurrentPage = signal<number>(0);
  ticketPageSize = signal<number>(10);
  ticketTotalPages = signal<number>(0);
  ticketTotalElements = signal<number>(0);

  // Filters
  ticketSearchTerm = signal<string>('');
  ticketStatusFilter = signal<string>('ALL');
  ticketPriorityFilter = signal<string>('ALL');
  selectedDateRange = signal<string>('all');
  customFromDate = signal<string>('');
  customToDate = signal<string>('');
  showFilters = signal<boolean>(false);
  showAnalyticsSection = signal<boolean>(true);

  readonly displayTickets = computed<TicketResponse[]>(() => this.serverTickets());

  ngOnInit(): void {
    this.state.loadUnreadCount();
    this.loadAllData();
  }

  loadAllData(): void {
    this.loadDashboardData();
    this.loadTicketsPage(0);
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
}
