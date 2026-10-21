import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { vi } from 'vitest';
import { AgentDashboard } from './agent-dashboard';
import { AnalyticsService } from '../../../../core/services/analytics.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ReportService } from '../../../../core/services/report.service';
import { DashboardAnalyticsResponse } from '../../../../core/models/dashboard.models';

describe('AgentDashboard', () => {
  let component: AgentDashboard;
  let fixture: ComponentFixture<AgentDashboard>;

  const mockAnalytics: Partial<DashboardAnalyticsResponse> = {
    employeeName: 'Sarah Connor',
    summary: {
      totalTickets: 25,
      openTickets: 5,
      inProgressTickets: 8,
      onHoldTickets: 2,
      resolvedTickets: 10,
      withdrawnTickets: 0,
      reopenedTickets: 0,
      overdueTickets: 2,
      slaBreachedTickets: 1,
      slaWarningTickets: 2,
      slaCompliantTickets: 22,
      slaComplianceRate: 88.0,
      avgResolutionHours: 3.5,
      assignedTickets: 15,
      unassignedTickets: 0
    },
    recentTickets: [
      {
        id: 201,
        ticketNumber: 'TKT-201',
        subject: 'VPN Connection Failure',
        categoryName: 'Network',
        subCategoryName: 'VPN',
        status: 'IN_PROGRESS',
        priority: 'CRITICAL',
        slaStatus: 'ACTIVE',
        requesterName: 'John Connor',
        assignedAgentName: 'Sarah Connor',
        departmentName: 'IT Support',
        createdAt: '2026-10-12T10:00:00Z'
      },
      {
        id: 202,
        ticketNumber: 'TKT-202',
        subject: 'Email Sync Issue',
        categoryName: 'Email',
        subCategoryName: 'Outlook',
        status: 'OPEN',
        priority: 'MEDIUM',
        slaStatus: 'ACTIVE',
        requesterName: 'Kyle Reese',
        assignedAgentName: 'Sarah Connor',
        departmentName: 'IT Support',
        createdAt: '2026-10-12T11:00:00Z'
      }
    ]
  };

  let analyticsServiceSpy: any;
  let authServiceSpy: any;
  let reportServiceSpy: any;

  beforeEach(async () => {
    analyticsServiceSpy = {
      getDashboardAnalytics: vi.fn().mockReturnValue(of(mockAnalytics))
    };

    authServiceSpy = {
      currentUser: vi.fn().mockReturnValue({ firstName: 'Sarah', lastName: 'Connor', role: 'AGENT' }),
      isAuthenticated: vi.fn().mockReturnValue(true),
      getRole: vi.fn().mockReturnValue('AGENT'),
      getEmployeeId: vi.fn().mockReturnValue(2)
    };

    reportServiceSpy = {
      exportSummaryReport: vi.fn(),
      downloadBlob: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [AgentDashboard],
      providers: [
        provideRouter([]),
        { provide: AnalyticsService, useValue: analyticsServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ReportService, useValue: reportServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AgentDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load real backend metrics on init', () => {
    expect(component.totalAssigned).toBe(15);
    expect(component.inProgress).toBe(8);
    expect(component.slaAtRisk).toBe(3); // 2 warning + 1 breached
    expect(component.resolvedToday).toBe(10);
    expect(component.agentName).toBe('Sarah Connor');
    expect(component.tickets.length).toBe(2);
  });

  it('should filter tickets by search term and status', () => {
    component.searchTerm = 'VPN';
    expect(component.filteredTickets.length).toBe(1);
    expect(component.filteredTickets[0].ticketNumber).toBe('TKT-201');

    component.searchTerm = '';
    component.statusFilter = 'IN_PROGRESS';
    expect(component.filteredTickets.length).toBe(1);

    component.statusFilter = 'NEW';
    expect(component.filteredTickets.length).toBe(1);
    expect(component.filteredTickets[0].ticketNumber).toBe('TKT-202');
  });

  it('should render Export Summary button on assigned tickets view for AGENT', async () => {
    component.setActiveNav('assigned-tickets');
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    const exportBtn = compiled.querySelector('#export-assigned-summary-btn');
    expect(exportBtn).toBeTruthy();
    expect(exportBtn?.textContent).toContain('Export Summary');
  });

  it('should call exportSummaryReport with AGENT_SUMMARY, EXCEL format, and active filters', () => {
    component.assignedTickets.set([
      {
        id: 1,
        ticketNumber: 'HD-2026-000001',
        subject: 'Hardware failure',
        status: 'IN_PROGRESS',
        priority: 'HIGH',
        category: { id: 1, name: 'IT' },
        createdAt: '2026-10-10'
      } as any
    ]);
    component.assignedTotalElements.set(1);
    component.assignedSearchTerm.set('Hardware');
    component.assignedStatusFilter.set('IN_PROGRESS');
    component.assignedPriorityFilter.set('HIGH');

    const mockBlob = new Blob(['xlsx-content'], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });
    const mockResponse = new HttpResponse({
      body: mockBlob,
      headers: undefined
    });
    reportServiceSpy.exportSummaryReport.mockReturnValue(of(mockResponse));

    component.exportAssignedSummary();

    expect(reportServiceSpy.exportSummaryReport).toHaveBeenCalledWith(
      expect.objectContaining({
        reportType: 'AGENT_SUMMARY',
        format: 'EXCEL',
        status: 'IN_PROGRESS',
        priority: 'HIGH',
        search: 'Hardware'
      })
    );
    expect(reportServiceSpy.downloadBlob).toHaveBeenCalled();
    expect(component.isExportingAssigned()).toBe(false);
  });

  it('should display error notification when no assigned tickets match filters', () => {
    component.assignedTickets.set([]);
    component.assignedTotalElements.set(0);

    const showNotifSpy = vi.spyOn(component.state, 'showNotification');
    component.exportAssignedSummary();

    expect(reportServiceSpy.exportSummaryReport).not.toHaveBeenCalled();
    expect(showNotifSpy).toHaveBeenCalledWith(
      'error',
      'No assigned tickets match the selected criteria to export.'
    );
    expect(component.isExportingAssigned()).toBe(false);
  });

  it('should handle backend HTTP 404 error and reset loading state', () => {
    component.assignedTickets.set([
      {
        id: 1,
        ticketNumber: 'HD-2026-000001',
        subject: 'VPN Issue',
        status: 'OPEN',
        priority: 'LOW',
        createdAt: '2026-10-10'
      } as any
    ]);
    component.assignedTotalElements.set(1);

    const errorResponse = new HttpErrorResponse({
      status: 404,
      statusText: 'Not Found',
      error: { message: 'No assigned tickets match the selected criteria to export.' }
    });
    reportServiceSpy.exportSummaryReport.mockReturnValue(throwError(() => errorResponse));

    const showNotifSpy = vi.spyOn(component.state, 'showNotification');
    component.exportAssignedSummary();

    expect(showNotifSpy).toHaveBeenCalledWith(
      'error',
      'No assigned tickets match the selected criteria to export.'
    );
    expect(component.isExportingAssigned()).toBe(false);
  });

  it('should prevent repeated export clicks while export is in progress', () => {
    component.isExportingAssigned.set(true);

    component.exportAssignedSummary();

    expect(reportServiceSpy.exportSummaryReport).not.toHaveBeenCalled();
  });
});

