import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { DashboardComponent } from './dashboard';
import { AuthService } from '../../core/services/auth.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { ReportService } from '../../core/services/report.service';
import { TicketService } from '../../core/services/ticket.service';
import { NotificationService } from '../../core/services/notification.service';
import { DashboardAnalyticsResponse } from '../../core/models/dashboard.models';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  const mockTicketResponse = {
    id: 101,
    ticketNumber: 'TKT-20261010-001',
    subject: 'VPN Connection Failure',
    description: 'Cannot connect to company VPN from home',
    status: 'OPEN',
    priority: 'HIGH',
    slaStatus: 'ACTIVE',
    reopenCount: 0,
    department: { id: 1, name: 'IT Support' },
    category: { id: 10, name: 'Network' },
    subCategory: { id: 100, name: 'VPN' },
    requester: { id: 2, name: 'Bob Builder', email: 'bob@company.com' },
    assignedAgent: { id: 3, name: 'Agent Alice', email: 'alice@helpdesk.com' },
    assignedManager: { id: 4, name: 'Manager Dan', email: 'dan@helpdesk.com' },
    createdAt: '2026-10-10T08:30:00Z',
    updatedAt: '2026-10-10T08:30:00Z'
  };

  const mockAnalytics: DashboardAnalyticsResponse = {
    role: 'ADMIN',
    employeeId: 1,
    employeeName: 'Admin User',
    employeeEmail: 'admin@helpdesk.com',
    designation: 'System Administrator',
    departmentName: 'IT Operations',
    departmentId: 1,
    summary: {
      totalTickets: 42,
      openTickets: 10,
      inProgressTickets: 12,
      onHoldTickets: 4,
      resolvedTickets: 14,
      withdrawnTickets: 2,
      reopenedTickets: 1,
      overdueTickets: 3,
      slaBreachedTickets: 3,
      slaWarningTickets: 2,
      slaCompliantTickets: 39,
      slaComplianceRate: 92.8,
      avgResolutionHours: 4.8,
      assignedTickets: 38,
      unassignedTickets: 4
    },
    statusDistribution: [
      { key: 'OPEN', label: 'Open', count: 10, percentage: 23.8 },
      { key: 'RESOLVED', label: 'Resolved', count: 14, percentage: 33.3 }
    ],
    priorityDistribution: [
      { key: 'CRITICAL', label: 'Critical', count: 5, percentage: 11.9 },
      { key: 'HIGH', label: 'High', count: 15, percentage: 35.7 }
    ],
    departmentDistribution: [
      { departmentId: 1, departmentName: 'IT Support', totalTickets: 30, openTickets: 8, resolvedTickets: 10 }
    ],
    categoryDistribution: [
      { key: 'Hardware', label: 'Hardware', count: 18, percentage: 42.9 }
    ],
    trends: [
      { period: '2026-10-09', createdCount: 5, resolvedCount: 3 },
      { period: '2026-10-10', createdCount: 7, resolvedCount: 4 }
    ],
    agentWorkload: [
      { agentId: 1, agentName: 'Agent Alice', email: 'alice@helpdesk.com', departmentName: 'IT', activeTickets: 6, resolvedTickets: 12, totalTickets: 6 }
    ],
    employeeStats: {
      totalEmployees: 50,
      activeEmployees: 48,
      inactiveEmployees: 1,
      onLeaveEmployees: 1
    },
    recentTickets: [
      {
        id: 101,
        ticketNumber: 'TKT-20261010-001',
        subject: 'VPN Connection Failure',
        categoryName: 'Network',
        subCategoryName: 'VPN',
        status: 'OPEN',
        priority: 'HIGH',
        slaStatus: 'ACTIVE',
        requesterName: 'Bob Builder',
        assignedAgentName: 'Agent Alice',
        departmentName: 'IT Operations',
        createdAt: '2026-10-10T08:30:00Z'
      }
    ],
    generatedAt: '2026-10-10T10:00:00Z'
  };

  let authServiceSpy: any;
  let analyticsServiceSpy: any;
  let reportServiceSpy: any;
  let ticketServiceSpy: any;
  let notificationServiceSpy: any;

  beforeEach(async () => {
    authServiceSpy = {
      isAuthenticated: vi.fn().mockReturnValue(true),
      getRole: vi.fn().mockReturnValue('ADMIN'),
      getEmployeeId: vi.fn().mockReturnValue(1),
      currentUser: vi.fn().mockReturnValue({
        id: 1,
        firstName: 'Admin',
        lastName: 'User',
        email: 'admin@helpdesk.com',
        role: 'ADMIN',
        employmentStatus: 'ACTIVE'
      }),
      loadProfile: vi.fn().mockReturnValue(of({})),
      logout: vi.fn()
    };

    analyticsServiceSpy = {
      getDashboardAnalytics: vi.fn().mockReturnValue(of(mockAnalytics))
    };

    reportServiceSpy = {
      exportSummaryReport: vi.fn().mockReturnValue(of({ body: new Blob() })),
      getSummaryReport: vi.fn().mockReturnValue(of({
        reportType: 'GENERAL_SUMMARY',
        format: 'JSON',
        generatedAt: '2026-10-12T10:00:00Z',
        role: 'ADMIN',
        employeeId: 1,
        employeeName: 'Admin User',
        totalRecords: 42,
        summary: mockAnalytics.summary,
        statusDistribution: mockAnalytics.statusDistribution,
        priorityDistribution: mockAnalytics.priorityDistribution
      })),
      downloadBlob: vi.fn()
    };

    ticketServiceSpy = {
      searchTickets: vi.fn().mockReturnValue(of({
        content: [mockTicketResponse],
        totalElements: 1,
        totalPages: 1,
        pageNumber: 0,
        pageSize: 10,
        first: true,
        last: true
      })),
      getDepartments: vi.fn().mockReturnValue(of([
        { id: 1, name: 'IT Support', code: 'IT' }
      ])),
      getCategories: vi.fn().mockReturnValue(of([
        { id: 10, departmentId: 1, name: 'Network' }
      ])),
      getSubCategories: vi.fn().mockReturnValue(of([
        { id: 100, categoryId: 10, name: 'VPN', priority: 'HIGH' }
      ])),
      getTicket: vi.fn().mockReturnValue(of(mockTicketResponse)),
      getMessages: vi.fn().mockReturnValue(of({
        content: [
          {
            id: 1,
            ticketId: 101,
            senderId: 3,
            senderName: 'Agent Alice',
            senderRole: 'AGENT',
            message: 'Checking logs now.',
            isInternal: false,
            createdAt: '2026-10-10T09:00:00Z'
          }
        ]
      })),
      getAttachments: vi.fn().mockReturnValue(of([
        {
          id: 5,
          ticketId: 101,
          fileName: 'vpn-error.png',
          fileType: 'image/png',
          fileSize: 2048,
          createdAt: '2026-10-10T08:31:00Z',
          uploadedByName: 'Bob Builder'
        }
      ])),
      getSla: vi.fn().mockReturnValue(of({
        id: 1,
        ticketId: 101,
        policyName: 'High Priority SLA',
        status: 'ACTIVE',
        allocatedMinutes: 240,
        cycleNumber: 1,
        slaStartAt: '2026-10-10T08:30:00Z',
        currentDeadlineAt: '2026-10-10T12:30:00Z'
      })),
      getHistory: vi.fn().mockReturnValue(of([
        {
          id: 1,
          ticketId: 101,
          eventType: 'STATUS_CHANGE',
          oldValue: 'NEW',
          newValue: 'OPEN',
          createdAt: '2026-10-10T08:30:00Z'
        }
      ])),
      createTicket: vi.fn().mockReturnValue(of(mockTicketResponse)),
      addMessage: vi.fn().mockReturnValue(of({
        id: 2,
        ticketId: 101,
        senderId: 1,
        senderName: 'Admin User',
        senderRole: 'ADMIN',
        message: 'Reply sent',
        isInternal: false,
        createdAt: '2026-10-10T09:30:00Z'
      })),
      downloadAttachment: vi.fn().mockReturnValue(of({ body: new Blob(['test content']), headers: { get: () => null } }))
    };

    notificationServiceSpy = {
      getNotifications: vi.fn().mockReturnValue(of({
        content: [
          {
            id: 1,
            recipientId: 1,
            ticketId: 101,
            ticketNumber: 'TKT-20261010-001',
            type: 'TICKET_ASSIGNED',
            title: 'Ticket Assigned',
            message: 'Assigned to you',
            read: false,
            createdAt: '2026-10-10T09:00:00Z'
          }
        ],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
        last: true
      })),
      getUnreadCount: vi.fn().mockReturnValue(of({ unreadCount: 1 })),
      markAsRead: vi.fn().mockReturnValue(of({
        id: 1,
        read: true
      }))
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([
          { path: 'dashboard', component: DashboardComponent },
          { path: 'tickets', component: DashboardComponent },
          { path: 'login', component: DashboardComponent }
        ]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AnalyticsService, useValue: analyticsServiceSpy },
        { provide: ReportService, useValue: reportServiceSpy },
        { provide: TicketService, useValue: ticketServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the dashboard component and load analytics data', () => {
    expect(component).toBeTruthy();
    expect(component.analytics()).toEqual(mockAnalytics);
    expect(component.isLoading()).toBe(false);
  });

  it('should render strictly two primary navigation entries: Dashboard and Tickets', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const navItems = compiled.querySelectorAll('.sidebar-nav .nav-item');
    expect(navItems.length).toBe(2);

    const navTexts = Array.from(navItems).map((el) => el.textContent?.trim());
    expect(navTexts).toContain('Dashboard');
    expect(navTexts).toContain('Tickets');

    // Ensure removed options do not exist
    expect(navTexts.some((t) => t?.includes('Asset Management'))).toBe(false);
    expect(navTexts.some((t) => t?.includes('Exit Management'))).toBe(false);
  });

  it('should render exactly 2 analytics dropdown options and toggle views correctly', () => {
    expect(component.selectedAnalyticsView()).toBe('OVERALL_OVERVIEW');

    const compiled = fixture.nativeElement as HTMLElement;
    const select = compiled.querySelector('#analytics-view-select') as HTMLSelectElement;
    expect(select).toBeTruthy();
    expect(select.options.length).toBe(2);

    const values = Array.from(select.options).map((opt) => opt.value);
    expect(values).toEqual(['OVERALL_OVERVIEW', 'TICKET_ANALYTICS']);

    // Toggle to Ticket Analytics
    component.setAnalyticsView('TICKET_ANALYTICS');
    expect(component.selectedAnalyticsView()).toBe('TICKET_ANALYTICS');

    // Toggle back to Overall Overview
    component.setAnalyticsView('OVERALL_OVERVIEW');
    expect(component.selectedAnalyticsView()).toBe('OVERALL_OVERVIEW');
  });

  it('should not render SLA Compliance Rate card or My Work/Quick Actions in Overall Dashboard View', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.my-work-card')).toBeNull();
    expect(compiled.querySelector('.quick-actions-card')).toBeNull();

    // SLA Compliance rate card removed from dashboard view metrics
    const metricLabels = Array.from(compiled.querySelectorAll('.metrics-row-cards .sub-lbl'))
      .map((el) => el.textContent?.trim());
    expect(metricLabels).not.toContain('SLA Compliance Rate');
  });

  it('should navigate from KPI cards to filtered tickets', () => {
    // Open tickets KPI card navigation
    component.navigateToTicketsWithFilter({ status: 'OPEN' });
    expect(component.activeNav()).toBe('helpdesk');
    expect(component.ticketStatusFilter()).toBe('OPEN');
    expect(ticketServiceSpy.searchTickets).toHaveBeenCalledWith(
      expect.objectContaining({
        status: 'OPEN',
        page: 0
      })
    );

    // In-progress tickets KPI card navigation
    component.navigateToTicketsWithFilter({ status: 'IN_PROGRESS' });
    expect(component.ticketStatusFilter()).toBe('IN_PROGRESS');
  });

  it('should open notification drawer, render items, and mark notifications as seen', () => {
    // Unread count loaded on init
    expect(component.unreadNotificationCount()).toBe(1);

    // Open drawer
    component.toggleNotificationDrawer();
    expect(component.showNotificationDrawer()).toBe(true);
    expect(notificationServiceSpy.getNotifications).toHaveBeenCalled();
    expect(component.notificationsList().length).toBe(1);

    // Mark as Seen
    component.markNotificationSeen(1);
    expect(notificationServiceSpy.markAsRead).toHaveBeenCalledWith(1);
    expect(component.unreadNotificationCount()).toBe(0);
    expect(component.notificationsList()[0].read).toBe(true);

    // Close drawer
    component.closeNotificationDrawer();
    expect(component.showNotificationDrawer()).toBe(false);
  });

  it('should filter recent tickets by search term', () => {
    component.ticketSearchTerm.set('VPN');
    expect(component.filteredTickets().length).toBe(1);

    component.ticketSearchTerm.set('NonExistentTerm');
    expect(component.filteredTickets().length).toBe(0);
  });

  it('should switch navigation views correctly via navigateToNav', () => {
    expect(component.activeNav()).toBe('dashboard');
    component.navigateToNav('tickets');
    expect(component.activeNav()).toBe('helpdesk');
    component.navigateToNav('dashboard');
    expect(component.activeNav()).toBe('dashboard');
  });

  it('should select and clear tickets for preview', () => {
    const ticket = mockAnalytics.recentTickets[0];
    component.selectTicket(ticket);
    expect(component.selectedTicketForPreview()).toBe(ticket);
    expect(ticketServiceSpy.getTicket).toHaveBeenCalledWith(ticket.id);

    component.clearSelectedTicket();
    expect(component.selectedTicketForPreview()).toBeNull();
    expect(component.fullSelectedTicket()).toBeNull();
  });

  it('should switch ticket detail preview tabs and load data', () => {
    const ticket = mockAnalytics.recentTickets[0];
    component.selectTicket(ticket);

    // Switch to conversation tab
    component.switchPreviewTab('conversation');
    expect(component.previewActiveTab()).toBe('conversation');

    // Switch to attachments tab
    component.switchPreviewTab('attachments');
    expect(component.previewActiveTab()).toBe('attachments');

    // Switch to SLA tab
    component.switchPreviewTab('sla');
    expect(component.previewActiveTab()).toBe('sla');

    // Switch to history tab
    component.switchPreviewTab('history');
    expect(component.previewActiveTab()).toBe('history');
  });

  it('should send a reply in ticket conversation', () => {
    const ticket = mockAnalytics.recentTickets[0];
    component.selectTicket(ticket);

    component.replyContent.set('Here is the latest update.');
    component.submitReply();

    expect(ticketServiceSpy.addMessage).toHaveBeenCalledWith(
      ticket.id,
      'Here is the latest update.',
      []
    );
    expect(component.replyContent()).toBe('');
  });

  it('should toggle filter drawer and analytics view', () => {
    expect(component.showFilters()).toBe(false);
    component.toggleFilters();
    expect(component.showFilters()).toBe(true);

    expect(component.showAnalyticsSection()).toBe(false);
    component.toggleAnalyticsSection();
    expect(component.showAnalyticsSection()).toBe(true);
  });

  it('should open and close the new ticket modal and cascade category loading', () => {
    component.openNewTicketModal();
    expect(component.showNewTicketModal()).toBe(true);
    expect(ticketServiceSpy.getDepartments).toHaveBeenCalled();

    // Cascaded selection
    component.onDepartmentSelect(1);
    expect(ticketServiceSpy.getCategories).toHaveBeenCalledWith(1);

    component.onCategorySelect(10);
    expect(ticketServiceSpy.getSubCategories).toHaveBeenCalledWith(10);

    component.onSubCategorySelect(100);
    expect(component.newTicketPriority()).toBe('HIGH');

    component.closeNewTicketModal();
    expect(component.showNewTicketModal()).toBe(false);
  });

  it('should validate ticket subject when submitting', () => {
    component.newTicketSubject.set('');
    component.submitNewTicket();
    expect(component.notification()?.type).toBe('error');

    component.newTicketSubject.set('Issue with VPN');
    component.submitNewTicket();
    expect(component.notification()?.type).toBe('success');
    expect(component.showNewTicketModal()).toBe(false);
  });

  it('should correctly format relative time', () => {
    expect(component.formatRelativeTime(undefined, 0)).toBe('Today');
    expect(component.formatRelativeTime(undefined, 1)).toBe('Yesterday');
    expect(component.formatRelativeTime(undefined, 2)).toBe('2 days ago');
    expect(component.formatRelativeTime(undefined, 3)).toBe('3 days ago');
    expect(component.formatRelativeTime(undefined, 4)).toBe('4 days ago');
    expect(component.formatRelativeTime('Yesterday')).toBe('Yesterday');
  });

  it('should render the report category select and export buttons in Tickets toolbar', () => {
    component.setActiveNav('helpdesk');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const select = compiled.querySelector('#report-type-select');
    const viewBtn = compiled.querySelector('#view-json-summary-btn');
    const exportBtn = compiled.querySelector('#export-excel-btn');
    expect(select).toBeTruthy();
    expect(viewBtn).toBeTruthy();
    expect(exportBtn).toBeTruthy();
    expect(component.permittedReportTypes().length).toBeGreaterThan(0);
  });

  it('should invoke exportSummaryReport with selected category and dates', () => {
    component.selectedReportType.set('TICKET_SUMMARY');
    component.exportSummaryToExcel();

    expect(reportServiceSpy.exportSummaryReport).toHaveBeenCalledWith(
      expect.objectContaining({
        reportType: 'TICKET_SUMMARY'
      })
    );
  });

  it('should fetch summary and open modal without raw JSON response payload', () => {
    component.fetchSummaryJson();
    expect(component.isLoadingReportJson()).toBe(false);
    expect(component.showReportJsonModal()).toBe(true);

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    // Verify raw JSON block is strictly absent
    expect(compiled.querySelector('.json-code-box')).toBeNull();
    // Verify formatted metrics grid is present
    expect(compiled.querySelector('.report-summary-metrics-grid')).toBeTruthy();
  });

  it('should execute server ticket search with pagination and filters', () => {
    component.onSearchInput('Hardware');
    expect(ticketServiceSpy.searchTickets).toHaveBeenCalledWith(
      expect.objectContaining({
        search: 'Hardware',
        page: 0
      })
    );

    component.clearSearch();
    expect(ticketServiceSpy.searchTickets).toHaveBeenCalledWith(
      expect.objectContaining({
        search: undefined,
        page: 0
      })
    );

    component.setTicketView('ASSIGNED');
    expect(component.selectedTicketView()).toBe('ASSIGNED');
  });

  it('should not show fallback mock tickets when backend tickets array is empty', () => {
    ticketServiceSpy.searchTickets.mockReturnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      pageNumber: 0,
      pageSize: 10,
      first: true,
      last: true
    }));
    component.serverTickets.set([]);
    component.analytics.set({
      ...mockAnalytics,
      recentTickets: []
    });
    component.setActiveNav('helpdesk');
    fixture.detectChanges();

    expect(component.displayTickets().length).toBe(0);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('No tickets match your filter criteria');
    expect(compiled.textContent).not.toContain('Alex Morgan');
  });

  describe('Amazon-style Status Tracking Timeline', () => {
    it('should return empty steps when no ticket is selected', () => {
      component.selectedTicketForPreview.set(null);
      expect(component.statusTimelineSteps()).toEqual([]);
    });

    it('should compute correct timeline for an OPEN ticket', () => {
      component.selectedTicketForPreview.set({
        id: 1,
        ticketNumber: 'TICK-101',
        subject: 'VPN Connection issue',
        status: 'OPEN',
        priority: 'HIGH'
      } as any);
      component.ticketHistory.set([
        { id: 1, eventType: 'TICKET_CREATED', newValue: 'OPEN', createdAt: '2026-10-10T10:00:00Z' }
      ]);

      const steps = component.statusTimelineSteps();
      expect(steps.length).toBe(3);
      expect(steps[0]).toEqual({
        status: 'OPEN',
        label: 'Open',
        state: 'current',
        isLineCompleted: false
      });
      expect(steps[1]).toEqual({
        status: 'IN_PROGRESS',
        label: 'In Progress',
        state: 'pending',
        isLineCompleted: false
      });
      expect(steps[2]).toEqual({
        status: 'RESOLVED',
        label: 'Resolved',
        state: 'pending',
        isLineCompleted: false
      });
    });

    it('should compute correct timeline for an IN_PROGRESS ticket with completed prior step', () => {
      component.selectedTicketForPreview.set({
        id: 2,
        ticketNumber: 'TICK-102',
        subject: 'Hardware issue',
        status: 'IN_PROGRESS',
        priority: 'MEDIUM'
      } as any);
      component.ticketHistory.set([
        { id: 1, eventType: 'TICKET_CREATED', newValue: 'OPEN', createdAt: '2026-10-10T10:00:00Z' },
        { id: 2, eventType: 'STATUS_CHANGED', oldValue: 'OPEN', newValue: 'IN_PROGRESS', createdAt: '2026-10-10T10:30:00Z' }
      ]);

      const steps = component.statusTimelineSteps();
      expect(steps.length).toBe(3);
      expect(steps[0]).toEqual({
        status: 'OPEN',
        label: 'Open',
        state: 'completed',
        isLineCompleted: true
      });
      expect(steps[1]).toEqual({
        status: 'IN_PROGRESS',
        label: 'In Progress',
        state: 'current',
        isLineCompleted: false
      });
      expect(steps[2]).toEqual({
        status: 'RESOLVED',
        label: 'Resolved',
        state: 'pending',
        isLineCompleted: false
      });
    });

    it('should compute correct timeline for a RESOLVED ticket with all steps completed', () => {
      component.selectedTicketForPreview.set({
        id: 3,
        ticketNumber: 'TICK-103',
        subject: 'Software access',
        status: 'RESOLVED',
        priority: 'LOW'
      } as any);
      component.ticketHistory.set([
        { id: 1, eventType: 'TICKET_CREATED', newValue: 'OPEN', createdAt: '2026-10-10T10:00:00Z' },
        { id: 2, eventType: 'STATUS_CHANGED', oldValue: 'OPEN', newValue: 'IN_PROGRESS', createdAt: '2026-10-10T10:30:00Z' },
        { id: 3, eventType: 'RESOLVED', oldValue: 'IN_PROGRESS', newValue: 'RESOLVED', createdAt: '2026-10-10T11:00:00Z' }
      ]);

      const steps = component.statusTimelineSteps();
      expect(steps.length).toBe(3);
      expect(steps[0].state).toBe('completed');
      expect(steps[0].isLineCompleted).toBe(true);
      expect(steps[1].state).toBe('completed');
      expect(steps[1].isLineCompleted).toBe(true);
      expect(steps[2].state).toBe('current');
      expect(steps[2].status).toBe('RESOLVED');
    });

    it('should handle ON_HOLD status with pending resolution', () => {
      component.selectedTicketForPreview.set({
        id: 4,
        ticketNumber: 'TICK-104',
        subject: 'Waiting for hardware parts',
        status: 'ON_HOLD',
        priority: 'MEDIUM'
      } as any);
      component.ticketHistory.set([
        { id: 1, eventType: 'TICKET_CREATED', newValue: 'OPEN', createdAt: '2026-10-10T10:00:00Z' },
        { id: 2, eventType: 'STATUS_CHANGED', oldValue: 'OPEN', newValue: 'IN_PROGRESS', createdAt: '2026-10-10T10:30:00Z' },
        { id: 3, eventType: 'HOLD', oldValue: 'IN_PROGRESS', newValue: 'ON_HOLD', createdAt: '2026-10-10T11:00:00Z' }
      ]);

      const steps = component.statusTimelineSteps();
      expect(steps.map(s => ({ status: s.status, state: s.state }))).toEqual([
        { status: 'OPEN', state: 'completed' },
        { status: 'IN_PROGRESS', state: 'completed' },
        { status: 'ON_HOLD', state: 'current' },
        { status: 'RESOLVED', state: 'pending' }
      ]);
    });

    it('should reflect actual workflow for REOPENED tickets without marking future step as completed', () => {
      component.selectedTicketForPreview.set({
        id: 5,
        ticketNumber: 'TICK-105',
        subject: 'Recurring email issue',
        status: 'REOPENED',
        priority: 'HIGH'
      } as any);
      component.ticketHistory.set([
        { id: 1, eventType: 'TICKET_CREATED', newValue: 'OPEN', createdAt: '2026-10-10T10:00:00Z' },
        { id: 2, eventType: 'STATUS_CHANGED', oldValue: 'OPEN', newValue: 'IN_PROGRESS', createdAt: '2026-10-10T10:30:00Z' },
        { id: 3, eventType: 'RESOLVED', oldValue: 'IN_PROGRESS', newValue: 'RESOLVED', createdAt: '2026-10-10T11:00:00Z' },
        { id: 4, eventType: 'REOPENED', oldValue: 'RESOLVED', newValue: 'REOPENED', createdAt: '2026-10-10T12:00:00Z' }
      ]);

      const steps = component.statusTimelineSteps();
      expect(steps.map(s => ({ status: s.status, state: s.state }))).toEqual([
        { status: 'OPEN', state: 'completed' },
        { status: 'IN_PROGRESS', state: 'completed' },
        { status: 'RESOLVED', state: 'completed' },
        { status: 'REOPENED', state: 'current' },
        { status: 'RESOLVED', state: 'pending' }
      ]);
      expect(steps[4].state).toBe('pending');
      expect(steps[3].isLineCompleted).toBe(false);
    });

    it('should terminate cleanly for WITHDRAWN tickets without pending steps', () => {
      component.selectedTicketForPreview.set({
        id: 6,
        ticketNumber: 'TICK-106',
        subject: 'Cancelled request',
        status: 'WITHDRAWN',
        priority: 'LOW'
      } as any);
      component.ticketHistory.set([
        { id: 1, eventType: 'TICKET_CREATED', newValue: 'OPEN', createdAt: '2026-10-10T10:00:00Z' },
        { id: 2, eventType: 'WITHDRAWN', oldValue: 'OPEN', newValue: 'WITHDRAWN', createdAt: '2026-10-10T10:15:00Z' }
      ]);

      const steps = component.statusTimelineSteps();
      expect(steps.map(s => ({ status: s.status, state: s.state }))).toEqual([
        { status: 'OPEN', state: 'completed' },
        { status: 'WITHDRAWN', state: 'current' }
      ]);
      expect(steps.some(s => s.state === 'pending')).toBe(false);
    });
  });
});
