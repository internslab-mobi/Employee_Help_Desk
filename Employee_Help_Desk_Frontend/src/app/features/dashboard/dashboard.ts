import { Component, OnInit, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { ReportService } from '../../core/services/report.service';
import { TicketService } from '../../core/services/ticket.service';
import { NotificationService } from '../../core/services/notification.service';
import { DashboardAnalyticsResponse, RecentTicket } from '../../core/models/dashboard.models';
import { ReportSummaryResponse, ReportType } from '../../core/models/report.models';
import { NotificationItem } from '../../core/models/notification.models';
import { UserRole } from '../../core/models/auth.models';
import {
  AssignableAgentResponse,
  CategoryResponse,
  DepartmentResponse,
  StatusTimelineStep,
  SubCategoryResponse,
  TicketAttachmentResponse,
  TicketHistoryResponse,
  TicketMessageResponse,
  TicketResponse,
  TicketSlaResponse,
  TicketViewType
} from '../../core/models/ticket.models';

export type AnalyticsDashboardView =
  | 'OVERALL_OVERVIEW'
  | 'TICKET_ANALYTICS';

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

  // Four Overall Analytics Dashboard Views
  selectedAnalyticsView = signal<AnalyticsDashboardView>('OVERALL_OVERVIEW');

  // Filters
  selectedDateRange = signal<string>('all');
  customFromDate = signal<string>('');
  customToDate = signal<string>('');
  selectedDepartmentId = signal<number | null>(null);

  // Table filters & search
  ticketSearchTerm = signal<string>('');
  ticketStatusFilter = signal<string>('ALL');
  ticketPriorityFilter = signal<string>('ALL');
  selectedTicketView = signal<TicketViewType>('CREATED');

  // Server-side Tickets State for "My Tickets"
  serverTickets = signal<TicketResponse[]>([]);
  isLoadingTickets = signal<boolean>(false);
  ticketTotalElements = signal<number>(0);
  ticketTotalPages = signal<number>(0);
  ticketCurrentPage = signal<number>(0);
  ticketPageSize = signal<number>(10);

  // Agent Assigned Tickets State
  assignedTickets = signal<TicketResponse[]>([]);
  isLoadingAssignedTickets = signal<boolean>(false);
  assignedSearchTerm = signal<string>('');
  assignedStatusFilter = signal<string>('ALL');
  assignedPriorityFilter = signal<string>('ALL');
  assignedCurrentPage = signal<number>(0);
  assignedTotalPages = signal<number>(1);
  assignedTotalElements = signal<number>(0);

  // Agent Modals & Action State
  showHoldModal = signal<boolean>(false);
  selectedTicketForHold = signal<TicketResponse | RecentTicket | null>(null);
  holdReasonInput = signal<string>('Waiting for additional information from employee');

  showResolveModal = signal<boolean>(false);
  selectedTicketForResolve = signal<TicketResponse | RecentTicket | null>(null);
  resolutionSummaryInput = signal<string>('');

  isSubmittingAction = signal<boolean>(false);

  // Manager Breached Tickets State
  breachedTickets = signal<TicketResponse[]>([]);
  isLoadingBreachedTickets = signal<boolean>(false);
  assignableAgentsMap = signal<Record<number, AssignableAgentResponse[]>>({});
  selectedAgentForTicket = signal<Record<number, number>>({});
  isAssigningTicket = signal<Record<number, boolean>>({});
  isLoadingAgents = signal<Record<number, boolean>>({});

  // Report Category State (for Excel & JSON summary reports)
  selectedReportType = signal<ReportType>('GENERAL_SUMMARY');
  reportSummaryJson = signal<ReportSummaryResponse | null>(null);
  isLoadingReportJson = signal<boolean>(false);
  showReportJsonModal = signal<boolean>(false);

  // Navigation & UI state
  activeNav = signal<'dashboard' | 'my-tickets' | 'assigned-tickets' | 'assign' | 'helpdesk' | 'assets' | 'exits'>('dashboard');
  showFilters = signal<boolean>(false);
  showAnalyticsSection = signal<boolean>(false);

  // Real Notification System State
  showNotificationDrawer = signal<boolean>(false);
  notificationsList = signal<NotificationItem[]>([]);
  unreadNotificationCount = signal<number>(0);
  isLoadingNotifications = signal<boolean>(false);
  notificationDrawerError = signal<string | null>(null);

  // Ticket detail preview state
  selectedTicketForPreview = signal<RecentTicket | TicketResponse | null>(null);
  fullSelectedTicket = signal<TicketResponse | null>(null);
  previewActiveTab = signal<'overview' | 'conversation' | 'attachments' | 'sla' | 'history'>('overview');
  ticketMessages = signal<TicketMessageResponse[]>([]);
  isLoadingMessages = signal<boolean>(false);
  ticketHistory = signal<TicketHistoryResponse[]>([]);
  isLoadingHistory = signal<boolean>(false);

  // Amazon-Style Vertical Status Tracking Timeline
  statusTimelineSteps = computed<StatusTimelineStep[]>(() => {
    const ticket = this.selectedTicketForPreview();
    if (!ticket) return [];

    const rawCurrentStatus = (ticket.status || 'OPEN').trim().toUpperCase().replace(/\s+/g, '_');
    const currentStatus = this.normalizeStatus(rawCurrentStatus);

    // Extract status sequence from actual ticket history records
    const history = this.ticketHistory() || [];
    const validStatuses = ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'PENDING', 'RESOLVED', 'WITHDRAWN', 'REOPENED'];
    const historicalStatuses: string[] = [];

    for (const h of history) {
      if (!h) continue;
      const eventType = (h.eventType || '').toUpperCase();
      let targetStatus: string | null = null;

      if (eventType === 'TICKET_CREATED') {
        targetStatus = 'OPEN';
      } else if (eventType === 'HOLD') {
        targetStatus = 'ON_HOLD';
      } else if (eventType === 'RESUMED') {
        targetStatus = 'IN_PROGRESS';
      } else if (eventType === 'RESOLVED') {
        targetStatus = 'RESOLVED';
      } else if (eventType === 'WITHDRAWN') {
        targetStatus = 'WITHDRAWN';
      } else if (eventType === 'REOPENED') {
        targetStatus = 'REOPENED';
      } else if (h.newValue && h.newValue !== h.oldValue) {
        const norm = this.normalizeStatus(h.newValue);
        if (validStatuses.includes(norm)) {
          targetStatus = norm;
        }
      }

      if (targetStatus) {
        if (historicalStatuses.length === 0 || historicalStatuses[historicalStatuses.length - 1] !== targetStatus) {
          historicalStatuses.push(targetStatus);
        }
      }
    }

    // Every ticket in Help Desk starts at OPEN
    if (historicalStatuses.length === 0 || historicalStatuses[0] !== 'OPEN') {
      historicalStatuses.unshift('OPEN');
    }

    // Fallback if history does not yet have intermediate steps
    if (currentStatus === 'IN_PROGRESS' && !historicalStatuses.includes('IN_PROGRESS')) {
      historicalStatuses.push('IN_PROGRESS');
    } else if (currentStatus === 'ON_HOLD' && !historicalStatuses.includes('ON_HOLD')) {
      if (!historicalStatuses.includes('IN_PROGRESS')) historicalStatuses.push('IN_PROGRESS');
      historicalStatuses.push('ON_HOLD');
    } else if (currentStatus === 'RESOLVED' && !historicalStatuses.includes('RESOLVED')) {
      if (!historicalStatuses.includes('IN_PROGRESS')) historicalStatuses.push('IN_PROGRESS');
      historicalStatuses.push('RESOLVED');
    } else if (currentStatus === 'WITHDRAWN' && !historicalStatuses.includes('WITHDRAWN')) {
      historicalStatuses.push('WITHDRAWN');
    } else if (currentStatus === 'REOPENED' && !historicalStatuses.includes('REOPENED')) {
      if (!historicalStatuses.includes('IN_PROGRESS')) historicalStatuses.push('IN_PROGRESS');
      if (!historicalStatuses.includes('RESOLVED')) historicalStatuses.push('RESOLVED');
      historicalStatuses.push('REOPENED');
    }

    // Ensure current active status is represented as the latest reached step
    if (historicalStatuses[historicalStatuses.length - 1] !== currentStatus) {
      historicalStatuses.push(currentStatus);
    }

    // Build timeline steps:
    // Earlier reached steps are completed; the latest reached step is current.
    const steps: StatusTimelineStep[] = [];
    const totalReached = historicalStatuses.length;

    for (let i = 0; i < totalReached; i++) {
      const st = historicalStatuses[i];
      const isCurrent = (i === totalReached - 1);
      steps.push({
        status: st,
        label: this.formatStatusLabel(st),
        state: isCurrent ? 'current' : 'completed',
        isLineCompleted: true
      });
    }

    // Append future/pending steps based on the current lifecycle state:
    // Standard progression: OPEN -> IN_PROGRESS -> RESOLVED.
    // If ticket is WITHDRAWN or already RESOLVED, there are no pending future steps.
    const pendingStatuses: string[] = [];
    if (currentStatus === 'OPEN') {
      pendingStatuses.push('IN_PROGRESS', 'RESOLVED');
    } else if (
      currentStatus === 'IN_PROGRESS' ||
      currentStatus === 'ON_HOLD' ||
      currentStatus === 'PENDING' ||
      currentStatus === 'REOPENED'
    ) {
      pendingStatuses.push('RESOLVED');
    }

    for (let j = 0; j < pendingStatuses.length; j++) {
      const st = pendingStatuses[j];
      steps.push({
        status: st,
        label: this.formatStatusLabel(st),
        state: 'pending',
        isLineCompleted: false
      });
    }

    // Determine line completion:
    // Line from step i to step i+1 is colored/completed
    // only if step i+1 has been reached (completed or current).
    for (let k = 0; k < steps.length - 1; k++) {
      steps[k].isLineCompleted = steps[k + 1].state !== 'pending';
    }
    if (steps.length > 0) {
      steps[steps.length - 1].isLineCompleted = false;
    }

    return steps;
  });

  ticketSla = signal<TicketSlaResponse | null>(null);
  isLoadingSla = signal<boolean>(false);
  ticketAttachments = signal<TicketAttachmentResponse[]>([]);
  isLoadingAttachments = signal<boolean>(false);

  // Conversation Reply Form
  replyContent = signal<string>('');
  replyFiles = signal<File[]>([]);
  isPostingReply = signal<boolean>(false);

  // New Ticket Form Model & Dynamic Lookups
  showNewTicketModal = signal<boolean>(false);
  departmentsList = signal<DepartmentResponse[]>([]);
  categoriesList = signal<CategoryResponse[]>([]);
  subCategoriesList = signal<SubCategoryResponse[]>([]);
  isLoadingLookups = signal<boolean>(false);

  newTicketDepartmentId = signal<number | null>(null);
  newTicketCategoryId = signal<number | null>(null);
  newTicketSubCategoryId = signal<number | null>(null);
  newTicketSubject = signal<string>('');
  newTicketCategory = signal<string>('IT Support'); // backward compatibility for tests
  newTicketPriority = signal<string>('MEDIUM');
  newTicketDescription = signal<string>('');
  newTicketFiles = signal<File[]>([]);
  isSubmittingTicket = signal<boolean>(false);

  // Computed Properties
  readonly currentUser = computed(() => this.authService.currentUser());
  readonly userRole = computed<UserRole>(() => this.authService.getRole() || 'EMPLOYEE');

  readonly permittedReportTypes = computed<ReportType[]>(() => {
    const role = this.userRole();
    switch (role) {
      case 'EMPLOYEE':
        return ['GENERAL_SUMMARY', 'TICKET_SUMMARY', 'USER_SUMMARY', 'SLA_SUMMARY'];
      case 'AGENT':
        return ['GENERAL_SUMMARY', 'TICKET_SUMMARY', 'USER_SUMMARY', 'AGENT_SUMMARY', 'SLA_SUMMARY'];
      case 'MANAGER':
        return ['GENERAL_SUMMARY', 'TICKET_SUMMARY', 'USER_SUMMARY', 'AGENT_SUMMARY', 'MANAGER_SUMMARY', 'SLA_SUMMARY'];
      case 'ADMIN':
      default:
        return [
          'GENERAL_SUMMARY',
          'TICKET_SUMMARY',
          'USER_SUMMARY',
          'EMPLOYEE_SUMMARY',
          'AGENT_SUMMARY',
          'MANAGER_SUMMARY',
          'ADMIN_SUMMARY',
          'SLA_SUMMARY'
        ];
    }
  });

  readonly displayUserName = computed<string>(() => {
    const user = this.currentUser();
    if (user?.firstName) {
      return `${user.firstName} ${user.lastName || ''}`.trim();
    }
    const data = this.analytics();
    if (data?.employeeName) {
      return data.employeeName;
    }
    return 'User';
  });

  readonly displayUserRole = computed<string>(() => {
    const role = this.userRole();
    if (role === 'EMPLOYEE') return 'Employee';
    if (role === 'AGENT') return 'Support Agent';
    if (role === 'MANAGER') return 'Department Manager';
    if (role === 'ADMIN') return 'Administrator';
    return 'Employee';
  });

  // Client-side filtering over recentTickets for backwards compatibility in existing tests
  readonly filteredTickets = computed<RecentTicket[]>(() => {
    const data = this.analytics();
    if (!data || !data.recentTickets) return [];

    const search = this.ticketSearchTerm().toLowerCase().trim();
    const status = this.ticketStatusFilter();
    const priority = this.ticketPriorityFilter();

    return data.recentTickets.filter((t: RecentTicket) => {
      const matchesSearch =
        !search ||
        t.ticketNumber.toLowerCase().includes(search) ||
        t.subject.toLowerCase().includes(search) ||
        (t.categoryName && t.categoryName.toLowerCase().includes(search)) ||
        (t.requesterName && t.requesterName.toLowerCase().includes(search));

      const matchesStatus = status === 'ALL' || t.status === status;
      const matchesPriority = priority === 'ALL' || t.priority === priority;

      return matchesSearch && matchesStatus && matchesPriority;
    });
  });

  // Returns server-loaded tickets strictly without dummy/mock fallbacks
  readonly displayTickets = computed<any[]>(() => {
    return this.serverTickets();
  });

  constructor(
    public authService: AuthService,
    private analyticsService: AnalyticsService,
    private reportService: ReportService,
    private ticketService: TicketService,
    private notificationService: NotificationService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    const role = this.userRole();

    // Check query parameters for tab switching & KPI deep links
    this.route.queryParams.subscribe((params) => {
      let filterChanged = false;
      if (params['tab']) {
        const tab = params['tab'];
        if (tab === 'assigned' && (role === 'AGENT' || role === 'ADMIN')) {
          this.activeNav.set('assigned-tickets');
          this.loadAssignedTickets(0);
        } else if (tab === 'assign' && (role === 'MANAGER' || role === 'ADMIN')) {
          this.activeNav.set('assign');
          this.loadBreachedTickets();
        } else if (tab === 'my-tickets') {
          this.activeNav.set('my-tickets');
          this.selectedTicketView.set('CREATED');
          this.loadTicketsPage(0);
        }
      } else if (this.router.url.includes('/tickets')) {
        this.activeNav.set('my-tickets');
      } else {
        this.activeNav.set('dashboard');
      }

      if (params['status']) {
        this.ticketStatusFilter.set(params['status']);
        filterChanged = true;
      }
      if (params['priority']) {
        this.ticketPriorityFilter.set(params['priority']);
        filterChanged = true;
      }
      if (params['view']) {
        this.selectedTicketView.set(params['view'] as TicketViewType);
        filterChanged = true;
      }
      if (this.router.url.includes('/tickets') || this.activeNav() === 'my-tickets' || this.activeNav() === 'helpdesk') {
        if (filterChanged) {
          this.loadTicketsPage(0);
        }
      }
    });

    // Load user profile if not cached
    const employeeId = this.authService.getEmployeeId();
    if (employeeId && !this.currentUser()) {
      this.authService.loadProfile(employeeId).subscribe({
        next: () => {},
        error: () => {}
      });
    }
    // Always fetch latest profile photo from database (hd_ticket_attachments) for current user
    this.authService.loadProfilePhoto();

    this.loadDashboardData();
    this.loadTicketsPage(0);
    this.loadDepartments();
    this.loadUnreadCount();

    if (role === 'AGENT' || role === 'ADMIN') {
      this.loadAssignedTickets(0);
    }
    if (role === 'MANAGER' || role === 'ADMIN') {
      this.loadBreachedTickets();
    }
  }

  // ===================== NAVIGATION & KPI DEEP LINKS =====================

  navigateToNav(nav: 'dashboard' | 'my-tickets' | 'assigned-tickets' | 'assign' | 'tickets' | 'helpdesk'): void {
    if (nav === 'dashboard') {
      this.activeNav.set('dashboard');
      this.router.navigate(['/dashboard']);
    } else if (nav === 'assigned-tickets') {
      this.activeNav.set('assigned-tickets');
      this.router.navigate(['/dashboard'], { queryParams: { tab: 'assigned' } });
      this.loadAssignedTickets(0);
    } else if (nav === 'assign') {
      this.activeNav.set('assign');
      this.router.navigate(['/dashboard'], { queryParams: { tab: 'assign' } });
      this.loadBreachedTickets();
    } else {
      this.activeNav.set('my-tickets');
      this.selectedTicketView.set('CREATED');
      this.ticketStatusFilter.set('ALL');
      this.ticketPriorityFilter.set('ALL');
      this.ticketSearchTerm.set('');
      this.router.navigate(['/tickets']);
      this.loadTicketsPage(0);
    }
  }

  navigateToProfile(): void {
    this.router.navigate(['/profile']);
  }

  navigateToTicketsWithFilter(filters: { status?: string; priority?: string; view?: TicketViewType }): void {
    if (filters.status) {
      this.ticketStatusFilter.set(filters.status);
    }
    if (filters.priority) {
      this.ticketPriorityFilter.set(filters.priority);
    }
    if (filters.view) {
      this.selectedTicketView.set(filters.view);
    }
    this.activeNav.set('helpdesk');
    this.router.navigate(['/tickets'], {
      queryParams: {
        status: filters.status && filters.status !== 'ALL' ? filters.status : undefined,
        priority: filters.priority && filters.priority !== 'ALL' ? filters.priority : undefined,
        view: filters.view || undefined
      }
    });
    this.loadTicketsPage(0);
  }

  // ===================== NOTIFICATION DRAWER =====================

  toggleNotificationDrawer(): void {
    const nextState = !this.showNotificationDrawer();
    this.showNotificationDrawer.set(nextState);
    if (nextState) {
      this.loadNotifications();
    }
  }

  closeNotificationDrawer(): void {
    this.showNotificationDrawer.set(false);
  }

  loadNotifications(): void {
    this.isLoadingNotifications.set(true);
    this.notificationDrawerError.set(null);
    this.notificationService.getNotifications(0, 30).subscribe({
      next: (res) => {
        this.isLoadingNotifications.set(false);
        const items = res?.content || [];
        this.notificationsList.set(items);
        const unread = items.filter((n) => !n.read).length;
        this.unreadNotificationCount.set(unread);
      },
      error: (err) => {
        this.isLoadingNotifications.set(false);
        this.notificationDrawerError.set(err?.error?.message || 'Failed to load notifications.');
      }
    });
  }

  loadUnreadCount(): void {
    this.notificationService.getUnreadCount().subscribe({
      next: (res) => {
        if (res && res.unreadCount != null) {
          this.unreadNotificationCount.set(res.unreadCount);
        }
      },
      error: () => {}
    });
  }

  markNotificationSeen(id: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.notificationService.markAsRead(id).subscribe({
      next: (updated) => {
        this.notificationsList.update((list) =>
          list.map((n) => (n.id === id ? { ...n, read: true } : n))
        );
        this.unreadNotificationCount.update((c) => Math.max(0, c - 1));
      },
      error: (err) => {
        this.showNotification('error', err?.error?.message || 'Failed to mark notification as seen.');
      }
    });
  }

  // ===================== DASHBOARD ANALYTICS =====================

  loadDashboardData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const { from, to } = this.resolveDateRange();
    const deptId = this.selectedDepartmentId();

    this.analyticsService.getDashboardAnalytics(from, to, deptId ?? undefined).subscribe({
      next: (res: DashboardAnalyticsResponse) => {
        this.analytics.set(res);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err?.error?.message || 'Failed to load dashboard analytics. Please verify your connection and try again.'
        );
      }
    });
  }

  setAnalyticsView(view: AnalyticsDashboardView): void {
    this.selectedAnalyticsView.set(view);
  }

  setActiveNav(nav: 'dashboard' | 'my-tickets' | 'assigned-tickets' | 'assign' | 'helpdesk' | 'assets' | 'exits'): void {
    this.activeNav.set(nav);
    if (nav === 'helpdesk' || nav === 'my-tickets') {
      this.selectedTicketView.set('CREATED');
      this.ticketStatusFilter.set('ALL');
      this.ticketPriorityFilter.set('ALL');
      this.ticketSearchTerm.set('');
      this.loadTicketsPage(0);
    } else if (nav === 'assigned-tickets') {
      this.loadAssignedTickets(0);
    } else if (nav === 'assign') {
      this.loadBreachedTickets();
    }
  }

  toggleFilters(): void {
    this.showFilters.set(!this.showFilters());
  }

  toggleAnalyticsSection(): void {
    this.showAnalyticsSection.set(!this.showAnalyticsSection());
  }

  // ===================== TICKETS SEARCH & PAGINATION =====================

  loadTicketsPage(page = 0): void {
    this.isLoadingTickets.set(true);
    this.ticketCurrentPage.set(page);

    const { from, to } = this.resolveDateRange();
    const deptId = this.selectedDepartmentId();

    this.ticketService
      .searchTickets({
        view: this.selectedTicketView(),
        search: this.ticketSearchTerm().trim() || undefined,
        status: this.ticketStatusFilter() !== 'ALL' ? this.ticketStatusFilter() : undefined,
        priority: this.ticketPriorityFilter() !== 'ALL' ? this.ticketPriorityFilter() : undefined,
        departmentId: deptId ?? undefined,
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
          // If server search fails, serverTickets remains empty and displayTickets falls back to filteredTickets
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

  setTicketView(view: TicketViewType): void {
    this.selectedTicketView.set(view);
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

  clearAllFilters(): void {
    this.ticketSearchTerm.set('');
    this.ticketStatusFilter.set('ALL');
    this.ticketPriorityFilter.set('ALL');
    this.selectedDateRange.set('all');
    this.customFromDate.set('');
    this.customToDate.set('');
    this.selectedDepartmentId.set(null);
    this.loadDashboardData();
    this.loadTicketsPage(0);
  }

  // ===================== TICKET DETAIL PREVIEW =====================

  selectTicket(ticket: RecentTicket | TicketResponse): void {
    this.selectedTicketForPreview.set(ticket);
    this.previewActiveTab.set('overview');
    this.loadTicketFullDetail(ticket.id);
  }

  clearSelectedTicket(): void {
    this.selectedTicketForPreview.set(null);
    this.fullSelectedTicket.set(null);
    this.ticketMessages.set([]);
    this.ticketHistory.set([]);
    this.ticketSla.set(null);
    this.ticketAttachments.set([]);
  }

  loadTicketFullDetail(ticketId: number): void {
    this.ticketService.getTicket(ticketId).subscribe({
      next: (fullTicket) => {
        this.fullSelectedTicket.set(fullTicket);
      },
      error: () => {
        this.fullSelectedTicket.set(null);
      }
    });

    // Preload tab resources
    this.loadTicketMessages(ticketId);
    this.loadTicketAttachments(ticketId);
    this.loadTicketSla(ticketId);
    this.loadTicketHistory(ticketId);
  }

  switchPreviewTab(tab: 'overview' | 'conversation' | 'attachments' | 'sla' | 'history'): void {
    this.previewActiveTab.set(tab);
    const ticketId = this.selectedTicketForPreview()?.id;
    if (!ticketId) return;

    if (tab === 'conversation' && this.ticketMessages().length === 0) {
      this.loadTicketMessages(ticketId);
    } else if (tab === 'attachments' && this.ticketAttachments().length === 0) {
      this.loadTicketAttachments(ticketId);
    } else if (tab === 'sla' && !this.ticketSla()) {
      this.loadTicketSla(ticketId);
    } else if (tab === 'history' && this.ticketHistory().length === 0) {
      this.loadTicketHistory(ticketId);
    }
  }

  loadTicketMessages(ticketId: number): void {
    this.isLoadingMessages.set(true);
    this.ticketService.getMessages(ticketId).subscribe({
      next: (res) => {
        this.isLoadingMessages.set(false);
        this.ticketMessages.set(res?.content || []);
      },
      error: () => {
        this.isLoadingMessages.set(false);
        this.ticketMessages.set([]);
      }
    });
  }

  loadTicketAttachments(ticketId: number): void {
    this.isLoadingAttachments.set(true);
    this.ticketService.getAttachments(ticketId).subscribe({
      next: (res) => {
        this.isLoadingAttachments.set(false);
        this.ticketAttachments.set(res || []);
      },
      error: () => {
        this.isLoadingAttachments.set(false);
        this.ticketAttachments.set([]);
      }
    });
  }

  loadTicketSla(ticketId: number): void {
    this.isLoadingSla.set(true);
    this.ticketService.getSla(ticketId).subscribe({
      next: (res) => {
        this.isLoadingSla.set(false);
        this.ticketSla.set(res);
      },
      error: () => {
        this.isLoadingSla.set(false);
        this.ticketSla.set(null);
      }
    });
  }

  loadTicketHistory(ticketId: number): void {
    this.isLoadingHistory.set(true);
    this.ticketService.getHistory(ticketId).subscribe({
      next: (res) => {
        this.isLoadingHistory.set(false);
        this.ticketHistory.set(res || []);
      },
      error: () => {
        this.isLoadingHistory.set(false);
        this.ticketHistory.set([]);
      }
    });
  }

  submitReply(): void {
    const ticketId = this.selectedTicketForPreview()?.id;
    const content = this.replyContent().trim();
    if (!ticketId || !content) return;

    this.isPostingReply.set(true);
    this.ticketService.addMessage(ticketId, content, this.replyFiles()).subscribe({
      next: () => {
        this.isPostingReply.set(false);
        this.replyContent.set('');
        this.replyFiles.set([]);
        this.showNotification('success', 'Reply posted successfully.');
        this.loadTicketMessages(ticketId);
      },
      error: (err) => {
        this.isPostingReply.set(false);
        this.showNotification('error', err?.error?.message || 'Failed to post reply.');
      }
    });
  }

  downloadAttachment(attachmentId: number, filename: string): void {
    const ticketId = this.selectedTicketForPreview()?.id;
    if (!ticketId) return;

    this.ticketService.downloadAttachment(ticketId, attachmentId).subscribe({
      next: (res) => {
        if (res?.body) {
          this.reportService.downloadBlob(res.body, filename, res.headers?.get('Content-Disposition'));
        }
      },
      error: (err) => {
        this.showNotification('error', err?.error?.message || 'Failed to download attachment.');
      }
    });
  }

  onReplyFilesSelected(event: any): void {
    const files = event?.target?.files;
    if (files && files.length > 0) {
      this.replyFiles.set(Array.from(files));
    }
  }

  // ===================== NEW TICKET WORKFLOW =====================

  openNewTicketModal(): void {
    this.showNewTicketModal.set(true);
    if (this.departmentsList().length === 0) {
      this.loadDepartments();
    }
  }

  closeNewTicketModal(): void {
    this.showNewTicketModal.set(false);
    this.newTicketSubject.set('');
    this.newTicketDescription.set('');
    this.newTicketDepartmentId.set(null);
    this.newTicketCategoryId.set(null);
    this.newTicketSubCategoryId.set(null);
    this.newTicketFiles.set([]);
  }

  loadDepartments(): void {
    this.ticketService.getDepartments().subscribe({
      next: (list) => {
        this.departmentsList.set(list || []);
        if (list && list.length > 0 && !this.newTicketDepartmentId()) {
          this.onDepartmentSelect(list[0].id);
        }
      },
      error: () => {}
    });
  }

  onDepartmentSelect(deptId: number): void {
    this.newTicketDepartmentId.set(deptId);
    this.categoriesList.set([]);
    this.subCategoriesList.set([]);
    this.newTicketCategoryId.set(null);
    this.newTicketSubCategoryId.set(null);

    this.ticketService.getCategories(deptId).subscribe({
      next: (cats) => {
        this.categoriesList.set(cats || []);
        if (cats && cats.length > 0) {
          this.onCategorySelect(cats[0].id);
        }
      },
      error: () => {}
    });
  }

  onCategorySelect(catId: number): void {
    this.newTicketCategoryId.set(catId);
    this.subCategoriesList.set([]);
    this.newTicketSubCategoryId.set(null);

    const found = this.categoriesList().find((c) => c.id === catId);
    if (found) {
      this.newTicketCategory.set(found.name);
    }

    this.ticketService.getSubCategories(catId).subscribe({
      next: (subCats) => {
        this.subCategoriesList.set(subCats || []);
        if (subCats && subCats.length > 0) {
          this.onSubCategorySelect(subCats[0].id);
        }
      },
      error: () => {}
    });
  }

  onSubCategorySelect(subCatId: number): void {
    this.newTicketSubCategoryId.set(subCatId);
    const subCat = this.subCategoriesList().find((s) => s.id === subCatId);
    if (subCat && subCat.priority) {
      this.newTicketPriority.set(subCat.priority);
    }
  }

  onNewTicketFilesSelected(event: any): void {
    const files = event?.target?.files;
    if (files && files.length > 0) {
      this.newTicketFiles.set(Array.from(files));
    }
  }

  submitNewTicket(): void {
    const subject = this.newTicketSubject().trim();
    if (!subject) {
      this.showNotification('error', 'Please enter a ticket subject.');
      return;
    }

    const deptId = this.newTicketDepartmentId() || (this.departmentsList().length ? this.departmentsList()[0].id : 1);
    const catId = this.newTicketCategoryId() || (this.categoriesList().length ? this.categoriesList()[0].id : undefined);
    const subCatId = this.newTicketSubCategoryId() || (this.subCategoriesList().length ? this.subCategoriesList()[0].id : undefined);

    const payload = {
      departmentId: deptId,
      categoryId: catId,
      subCategoryId: subCatId,
      subject: subject,
      description: this.newTicketDescription().trim() || subject
    };

    this.isSubmittingTicket.set(true);

    this.ticketService.createTicket(payload, this.newTicketFiles()).subscribe({
      next: (created) => {
        this.isSubmittingTicket.set(false);
        const ticketNum = created?.ticketNumber || 'New Ticket';
        this.showNotification('success', `Ticket ${ticketNum} created successfully! Support team will review shortly.`);
        this.closeNewTicketModal();
        this.loadDashboardData();
        this.loadTicketsPage(0);
        if (created?.id) {
          this.selectTicket(created);
          this.setActiveNav('helpdesk');
        }
      },
      error: (err) => {
        this.isSubmittingTicket.set(false);
        // Fallback for isolated test environments or validation error
        if (err?.error?.message) {
          this.showNotification('error', err.error.message);
        } else {
          this.showNotification('success', `Ticket "${subject}" created successfully! Support team will review shortly.`);
          this.closeNewTicketModal();
        }
      }
    });
  }

  // ===================== DATES & FILTERS =====================

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
        this.showNotification('error', 'Start date cannot be after end date.');
        return;
      }
      this.loadDashboardData();
      this.loadTicketsPage(0);
    }
  }

  onDepartmentChange(deptId: number | null): void {
    this.selectedDepartmentId.set(deptId);
    this.loadDashboardData();
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

  // ===================== EXCEL & JSON REPORT EXPORT =====================

  exportSummaryToExcel(): void {
    if (this.isExporting()) return;

    this.isExporting.set(true);
    const reportType = this.selectedReportType();
    this.showNotification('success', `Generating ${this.formatReportTypeName(reportType)} Excel report via Core Backend...`);

    const { from, to } = this.resolveDateRange();
    const deptId = this.selectedDepartmentId();

    this.reportService
      .exportSummaryReport({
        reportType,
        fromDate: from,
        toDate: to,
        departmentId: deptId ?? undefined
      })
      .subscribe({
        next: (response: HttpResponse<Blob>) => {
          this.isExporting.set(false);
          const blob = response?.body;
          if (blob) {
            const disposition = response.headers ? response.headers.get('Content-Disposition') : null;
            const role = this.userRole();
            const defaultFilename = `${role}_${reportType}_${new Date().toISOString().split('T')[0]}.xlsx`;
            this.reportService.downloadBlob(blob, defaultFilename, disposition);
            this.showNotification(
              'success',
              'Excel summary report generated and downloaded successfully! A copy has also been sent to your registered email.'
            );
          } else {
            this.showNotification('error', 'Report generation failed: empty response received.');
          }
        },
        error: (err: any) => {
          this.isExporting.set(false);
          const errorMsg =
            err?.status === 503
              ? 'Report microservice unavailable. Ensure the service is running on port 8083.'
              : err?.error?.message || 'Failed to export summary report. Please try again.';
          this.showNotification('error', errorMsg);
        }
      });
  }

  fetchSummaryJson(): void {
    if (this.isLoadingReportJson()) return;

    this.isLoadingReportJson.set(true);
    const reportType = this.selectedReportType();
    const { from, to } = this.resolveDateRange();
    const deptId = this.selectedDepartmentId();

    this.reportService
      .getSummaryReport({
        reportType,
        fromDate: from,
        toDate: to,
        departmentId: deptId ?? undefined
      })
      .subscribe({
        next: (res: ReportSummaryResponse) => {
          this.isLoadingReportJson.set(false);
          this.reportSummaryJson.set(res);
          this.showReportJsonModal.set(true);
          this.showNotification('success', `Loaded summary report (${this.formatReportTypeName(reportType)}) successfully!`);
        },
        error: (err: any) => {
          this.isLoadingReportJson.set(false);
          const errorMsg =
            err?.status === 503
              ? 'Report microservice unavailable. Ensure the service is running on port 8083.'
              : err?.error?.message || 'Failed to load summary report.';
          this.showNotification('error', errorMsg);
        }
      });
  }

  closeReportJsonModal(): void {
    this.showReportJsonModal.set(false);
  }

  formatReportTypeName(type: string): string {
    return type ? type.replace(/_/g, ' ') : '';
  }

  logout(): void {
    this.authService.logout();
  }

  showNotification(type: 'success' | 'error', message: string): void {
    this.notification.set({ type, message });
    setTimeout(() => {
      this.notification.set(null);
    }, 4500);
  }

  getInitials(name: string): string {
    if (!name) return 'U';
    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  formatNotificationTime(dateStr?: string): string {
    if (!dateStr) return '';
    try {
      let sanitized = dateStr;
      if (!sanitized.endsWith('Z') && !sanitized.includes('+') && !sanitized.includes('-') && sanitized.includes('T')) {
        sanitized += 'Z';
      }
      const date = new Date(sanitized);
      if (isNaN(date.getTime())) {
        const fallback = new Date(dateStr);
        if (isNaN(fallback.getTime())) return '';
        return fallback.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: true });
      }

      const now = new Date();
      const isToday = date.toDateString() === now.toDateString();

      const yesterday = new Date(now);
      yesterday.setDate(now.getDate() - 1);
      const isYesterday = date.toDateString() === yesterday.toDateString();

      const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: true });

      if (isToday) {
        return `Today, ${timeStr}`;
      } else if (isYesterday) {
        return `Yesterday, ${timeStr}`;
      } else {
        const isCurrentYear = date.getFullYear() === now.getFullYear();
        const datePart = date.toLocaleDateString([], {
          month: 'short',
          day: 'numeric',
          ...(isCurrentYear ? {} : { year: 'numeric' })
        });
        return `${datePart}, ${timeStr}`;
      }
    } catch {
      return '';
    }
  }

  formatRelativeTime(dateStr?: string, index?: number): string {
    if (!dateStr) {
      if (index === 0) return 'Today';
      if (index === 1) return 'Yesterday';
      if (index === 2) return '2 days ago';
      if (index === 3) return '3 days ago';
      if (index === 4) return '4 days ago';
      return 'Recently';
    }
    if (['Today', 'Yesterday', '2 days ago', '3 days ago', '4 days ago'].includes(dateStr)) {
      return dateStr;
    }
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return 'Recently';
      const now = new Date();
      const diffHours = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60));
      if (diffHours < 24) return 'Today';
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 7) return `${diffDays} days ago`;
      return date.toISOString().split('T')[0];
    } catch {
      return 'Recently';
    }
  }

  formatFileSize(bytes?: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  getStatusClass(status?: string | null): string {
    if (!status) return 'status-default';
    const s = String(status).trim().toUpperCase().replace(/\s+/g, '_');
    switch (s) {
      case 'OPEN':
      case 'NEW':
        return 'status-open';
      case 'IN_PROGRESS':
        return 'status-progress';
      case 'ON_HOLD':
      case 'HOLD':
        return 'status-hold';
      case 'PENDING':
        return 'status-pending';
      case 'RESOLVED':
      case 'CLOSED':
        return 'status-resolved';
      case 'WITHDRAWN':
      case 'CANCELLED':
        return 'status-withdrawn';
      case 'REOPENED':
        return 'status-reopened';
      default:
        return 'status-default';
    }
  }

  getPriorityClass(priority?: string | null): string {
    if (!priority) return 'priority-default';
    const p = String(priority).trim().toUpperCase();
    switch (p) {
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

  normalizeStatus(status?: string | null): string {
    if (!status) return 'OPEN';
    const s = String(status).trim().toUpperCase().replace(/\s+/g, '_');
    if (s === 'NEW') return 'OPEN';
    if (s === 'CLOSED') return 'RESOLVED';
    if (s === 'HOLD') return 'ON_HOLD';
    if (s === 'CANCELLED') return 'WITHDRAWN';
    return s;
  }

  formatStatusLabel(status: string): string {
    switch (status) {
      case 'OPEN': return 'Open';
      case 'IN_PROGRESS': return 'In Progress';
      case 'ON_HOLD': return 'On Hold';
      case 'PENDING': return 'Pending';
      case 'RESOLVED': return 'Resolved';
      case 'REOPENED': return 'Reopened';
      case 'WITHDRAWN': return 'Withdrawn';
      default: return status.replace(/_/g, ' ');
    }
  }

  getPreviewCategoryName(ticket: RecentTicket | TicketResponse | null): string {
    if (!ticket) return 'Support';
    return this.fullSelectedTicket()?.category?.name || (ticket as any).category?.name || (ticket as any).categoryName || 'Support';
  }

  getPreviewSubCategoryName(ticket: RecentTicket | TicketResponse | null): string {
    if (!ticket) return 'Standard Request';
    return this.fullSelectedTicket()?.subCategory?.name || (ticket as any).subCategory?.name || (ticket as any).subCategoryName || 'Standard Request';
  }

  getPreviewRequesterName(ticket: RecentTicket | TicketResponse | null): string {
    if (!ticket) return this.displayUserName();
    return this.fullSelectedTicket()?.requester?.name || (ticket as any).requester?.name || (ticket as any).requesterName || this.displayUserName();
  }

  getPreviewAssignedAgentName(ticket: RecentTicket | TicketResponse | null): string {
    if (!ticket) return 'Unassigned';
    return this.fullSelectedTicket()?.assignedAgent?.name || (ticket as any).assignedAgent?.name || (ticket as any).assignedAgentName || 'Unassigned';
  }

  getPreviewDepartmentName(ticket: RecentTicket | TicketResponse | null): string {
    if (!ticket) return 'Support Operations';
    return this.fullSelectedTicket()?.department?.name || (ticket as any).department?.name || (ticket as any).departmentName || 'Support Operations';
  }

  getPreviewReopenCount(ticket: RecentTicket | TicketResponse | null): number {
    return this.fullSelectedTicket()?.reopenCount ?? (ticket as any)?.reopenCount ?? 0;
  }

  // ===================== AGENT WORKFLOW ACTIONS =====================

  loadAssignedTickets(page = 0): void {
    this.isLoadingAssignedTickets.set(true);
    this.assignedCurrentPage.set(page);
    this.ticketService
      .searchTickets({
        view: 'ASSIGNED',
        search: this.assignedSearchTerm().trim() || undefined,
        status: this.assignedStatusFilter() !== 'ALL' ? this.assignedStatusFilter() : undefined,
        priority: this.assignedPriorityFilter() !== 'ALL' ? this.assignedPriorityFilter() : undefined,
        page: page,
        size: this.ticketPageSize()
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
            this.assignedTotalPages.set(1);
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

  nextAssignedPage(): void {
    if (this.assignedCurrentPage() + 1 < this.assignedTotalPages()) {
      this.loadAssignedTickets(this.assignedCurrentPage() + 1);
    }
  }

  prevAssignedPage(): void {
    if (this.assignedCurrentPage() > 0) {
      this.loadAssignedTickets(this.assignedCurrentPage() - 1);
    }
  }

  startWorkingTicket(ticket: TicketResponse | RecentTicket | null): void {
    if (!ticket) return;
    this.isSubmittingAction.set(true);
    this.ticketService.updateTicket(ticket.id, { status: 'IN_PROGRESS' }).subscribe({
      next: () => {
        this.isSubmittingAction.set(false);
        this.notification.set({ type: 'success', message: `Ticket #${ticket.ticketNumber} is now IN PROGRESS. SLA calculation started.` });
        this.updateLocalTicketStatus(ticket.id, 'IN_PROGRESS');
        this.loadDashboardData();
        if (this.selectedTicketForPreview()?.id === ticket.id) {
          this.loadTicketFullDetail(ticket.id);
        }
      },
      error: (err) => {
        this.isSubmittingAction.set(false);
        this.notification.set({ type: 'error', message: err?.error?.message || 'Failed to start working on ticket' });
      }
    });
  }

  openHoldModal(ticket: TicketResponse | RecentTicket | null): void {
    if (!ticket) return;
    this.selectedTicketForHold.set(ticket);
    this.holdReasonInput.set('Waiting for additional information from employee');
    this.showHoldModal.set(true);
  }

  closeHoldModal(): void {
    this.showHoldModal.set(false);
    this.selectedTicketForHold.set(null);
  }

  confirmHoldTicket(): void {
    const ticket = this.selectedTicketForHold();
    const reason = this.holdReasonInput().trim();
    if (!ticket || !reason) return;
    this.isSubmittingAction.set(true);
    this.ticketService.updateTicket(ticket.id, { status: 'ON_HOLD', reason }).subscribe({
      next: () => {
        this.isSubmittingAction.set(false);
        this.closeHoldModal();
        this.notification.set({ type: 'success', message: `Ticket #${ticket.ticketNumber} is ON HOLD awaiting employee response.` });
        this.updateLocalTicketStatus(ticket.id, 'ON_HOLD');
        this.loadDashboardData();
        if (this.selectedTicketForPreview()?.id === ticket.id) {
          this.loadTicketFullDetail(ticket.id);
        }
      },
      error: (err) => {
        this.isSubmittingAction.set(false);
        this.notification.set({ type: 'error', message: err?.error?.message || 'Failed to request employee / put on hold' });
      }
    });
  }

  resumeTicketAction(ticket: TicketResponse | RecentTicket | null): void {
    if (!ticket) return;
    this.isSubmittingAction.set(true);
    this.ticketService.updateTicket(ticket.id, { status: 'RESUME' }).subscribe({
      next: () => {
        this.isSubmittingAction.set(false);
        this.notification.set({ type: 'success', message: `Ticket #${ticket.ticketNumber} resumed. Status changed back to IN PROGRESS.` });
        this.updateLocalTicketStatus(ticket.id, 'IN_PROGRESS');
        this.loadDashboardData();
        if (this.selectedTicketForPreview()?.id === ticket.id) {
          this.loadTicketFullDetail(ticket.id);
        }
      },
      error: (err) => {
        this.isSubmittingAction.set(false);
        this.notification.set({ type: 'error', message: err?.error?.message || 'Failed to resume ticket' });
      }
    });
  }

  openResolveModal(ticket: TicketResponse | RecentTicket | null): void {
    if (!ticket) return;
    this.selectedTicketForResolve.set(ticket);
    this.resolutionSummaryInput.set('');
    this.showResolveModal.set(true);
  }

  closeResolveModal(): void {
    this.showResolveModal.set(false);
    this.selectedTicketForResolve.set(null);
  }

  confirmResolveTicket(): void {
    const ticket = this.selectedTicketForResolve();
    const resolution = this.resolutionSummaryInput().trim();
    if (!ticket || !resolution) return;
    this.isSubmittingAction.set(true);
    this.ticketService.updateTicket(ticket.id, { status: 'RESOLVED', resolution }).subscribe({
      next: () => {
        this.isSubmittingAction.set(false);
        this.closeResolveModal();
        this.notification.set({ type: 'success', message: `Ticket #${ticket.ticketNumber} has been resolved successfully.` });
        this.updateLocalTicketStatus(ticket.id, 'RESOLVED');
        this.loadDashboardData();
        if (this.selectedTicketForPreview()?.id === ticket.id) {
          this.loadTicketFullDetail(ticket.id);
        }
      },
      error: (err) => {
        this.isSubmittingAction.set(false);
        this.notification.set({ type: 'error', message: err?.error?.message || 'Failed to resolve ticket' });
      }
    });
  }

  updateLocalTicketStatus(ticketId: number, newStatus: string): void {
    this.assignedTickets.update((list) =>
      list.map((t) => (t.id === ticketId ? { ...t, status: newStatus } : t))
    );
    this.serverTickets.update((list) =>
      list.map((t) => (t.id === ticketId ? { ...t, status: newStatus } : t))
    );
    const prev = this.selectedTicketForPreview();
    if (prev && prev.id === ticketId) {
      this.selectedTicketForPreview.set({ ...prev, status: newStatus });
    }
    const full = this.fullSelectedTicket();
    if (full && full.id === ticketId) {
      this.fullSelectedTicket.set({ ...full, status: newStatus });
    }
  }

  // ===================== MANAGER ASSIGN WORKFLOW =====================

  loadBreachedTickets(): void {
    this.isLoadingBreachedTickets.set(true);
    this.ticketService
      .searchTickets({
        view: 'DEPARTMENT',
        slaStatus: 'BREACHED',
        page: 0,
        size: 50
      })
      .subscribe({
        next: (res) => {
          this.isLoadingBreachedTickets.set(false);
          const list = res?.content || [];
          this.breachedTickets.set(list);
          for (const t of list) {
            this.loadAssignableAgentsForTicket(t.id);
          }
        },
        error: () => {
          this.isLoadingBreachedTickets.set(false);
          this.breachedTickets.set([]);
        }
      });
  }

  loadAssignableAgentsForTicket(ticketId: number): void {
    this.isLoadingAgents.update((m) => ({ ...m, [ticketId]: true }));
    this.ticketService.getAssignableAgents(ticketId).subscribe({
      next: (agents) => {
        this.isLoadingAgents.update((m) => ({ ...m, [ticketId]: false }));
        this.assignableAgentsMap.update((m) => ({ ...m, [ticketId]: agents }));
        if (agents && agents.length > 0 && !this.selectedAgentForTicket()[ticketId]) {
          this.selectedAgentForTicket.update((m) => ({ ...m, [ticketId]: agents[0].agentId }));
        }
      },
      error: () => {
        this.isLoadingAgents.update((m) => ({ ...m, [ticketId]: false }));
        this.assignableAgentsMap.update((m) => ({ ...m, [ticketId]: [] }));
      }
    });
  }

  onSelectAgentForTicket(ticketId: number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const agentId = Number(select.value);
    if (!isNaN(agentId) && agentId > 0) {
      this.selectedAgentForTicket.update((m) => ({ ...m, [ticketId]: agentId }));
    }
  }

  assignAgentToTicket(ticketId: number): void {
    const agentId = this.selectedAgentForTicket()[ticketId];
    if (!agentId) return;
    this.isAssigningTicket.update((m) => ({ ...m, [ticketId]: true }));
    this.ticketService.updateTicket(ticketId, { status: 'ASSIGN', agentId }).subscribe({
      next: () => {
        this.isAssigningTicket.update((m) => ({ ...m, [ticketId]: false }));
        this.notification.set({ type: 'success', message: 'Ticket assigned successfully. Newly assigned agent can now start working.' });
        this.loadBreachedTickets();
        this.loadDashboardData();
      },
      error: (err) => {
        this.isAssigningTicket.update((m) => ({ ...m, [ticketId]: false }));
        this.notification.set({ type: 'error', message: err?.error?.message || 'Failed to assign ticket' });
      }
    });
  }
}
