import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { DashboardComponent } from './dashboard';
import { AuthService } from '../../core/services/auth.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { ReportService } from '../../core/services/report.service';
import { DashboardAnalyticsResponse } from '../../core/models/dashboard.models';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

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

  beforeEach(async () => {
    const authServiceSpy = {
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

    const analyticsServiceSpy = {
      getDashboardAnalytics: vi.fn().mockReturnValue(of(mockAnalytics))
    };

    const reportServiceSpy = {
      exportSummaryReport: vi.fn().mockReturnValue(of({ body: new Blob() })),
      downloadBlob: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AnalyticsService, useValue: analyticsServiceSpy },
        { provide: ReportService, useValue: reportServiceSpy }
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

  it('should render the export summary button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const exportBtn = compiled.querySelector('#export-excel-btn');
    expect(exportBtn).toBeTruthy();
    expect(exportBtn?.textContent).toContain('Export Summary to Excel');
  });

  it('should filter recent tickets by search term', () => {
    component.ticketSearchTerm.set('VPN');
    expect(component.filteredTickets().length).toBe(1);

    component.ticketSearchTerm.set('NonExistentTerm');
    expect(component.filteredTickets().length).toBe(0);
  });
});
