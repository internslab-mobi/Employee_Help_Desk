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

describe('DashboardComponent (Role Dispatcher)', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  let authServiceSpy: any;
  let analyticsServiceSpy: any;
  let reportServiceSpy: any;
  let ticketServiceSpy: any;
  let notificationServiceSpy: any;

  beforeEach(async () => {
    authServiceSpy = {
      isAuthenticated: vi.fn().mockReturnValue(true),
      getRole: vi.fn().mockReturnValue('EMPLOYEE'),
      getEmployeeId: vi.fn().mockReturnValue(1),
      currentUser: vi.fn().mockReturnValue({
        id: 1,
        firstName: 'Test',
        lastName: 'User',
        email: 'test@helpdesk.com',
        role: 'EMPLOYEE',
        employmentStatus: 'ACTIVE'
      }),
      loadProfile: vi.fn().mockReturnValue(of({})),
      logout: vi.fn()
    };

    analyticsServiceSpy = {
      getDashboardAnalytics: vi.fn().mockReturnValue(of({
        role: 'EMPLOYEE',
        employeeId: 1,
        summary: {},
        statusDistribution: [],
        priorityDistribution: [],
        departmentDistribution: [],
        categoryDistribution: [],
        trends: [],
        agentWorkload: [],
        employeeStats: {},
        recentTickets: []
      }))
    };

    reportServiceSpy = {
      exportSummaryReport: vi.fn().mockReturnValue(of({ body: new Blob() })),
      getSummaryReport: vi.fn().mockReturnValue(of({}))
    };

    ticketServiceSpy = {
      searchTickets: vi.fn().mockReturnValue(of({ content: [], totalElements: 0, totalPages: 0 })),
      getDepartments: vi.fn().mockReturnValue(of([])),
      getCategories: vi.fn().mockReturnValue(of([])),
      getSubCategories: vi.fn().mockReturnValue(of([])),
      getTicket: vi.fn().mockReturnValue(of({})),
      getMessages: vi.fn().mockReturnValue(of({ content: [] })),
      getAttachments: vi.fn().mockReturnValue(of([])),
      getSla: vi.fn().mockReturnValue(of({})),
      getHistory: vi.fn().mockReturnValue(of([])),
      createTicket: vi.fn().mockReturnValue(of({})),
      getAssignableAgents: vi.fn().mockReturnValue(of([]))
    };

    notificationServiceSpy = {
      getNotifications: vi.fn().mockReturnValue(of({ content: [], totalElements: 0, totalPages: 0 })),
      getUnreadCount: vi.fn().mockReturnValue(of({ unreadCount: 0 })),
      markAsRead: vi.fn().mockReturnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AnalyticsService, useValue: analyticsServiceSpy },
        { provide: ReportService, useValue: reportServiceSpy },
        { provide: TicketService, useValue: ticketServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    }).compileComponents();
  });

  it('should create the dashboard dispatcher component', () => {
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should render Employee Dashboard for EMPLOYEE role', () => {
    authServiceSpy.getRole.mockReturnValue('EMPLOYEE');
    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-employee-dashboard')).toBeTruthy();
  });

  it('should render Agent Dashboard for AGENT role', () => {
    authServiceSpy.getRole.mockReturnValue('AGENT');
    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-agent-dashboard')).toBeTruthy();
  });

  it('should render Manager Dashboard for MANAGER role', () => {
    authServiceSpy.getRole.mockReturnValue('MANAGER');
    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-manager-dashboard')).toBeTruthy();
  });

  it('should render Admin Dashboard for ADMIN role', () => {
    authServiceSpy.getRole.mockReturnValue('ADMIN');
    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-admin-dashboard')).toBeTruthy();
  });
});
