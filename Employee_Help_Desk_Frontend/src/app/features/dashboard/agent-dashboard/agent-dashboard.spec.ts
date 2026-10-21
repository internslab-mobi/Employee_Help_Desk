import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AgentDashboard } from './agent-dashboard';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { AuthService } from '../../../core/services/auth.service';
import { DashboardAnalyticsResponse } from '../../../core/models/dashboard.models';

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

  const analyticsServiceSpy = {
    getDashboardAnalytics: vi.fn().mockReturnValue(of(mockAnalytics))
  };

  const authServiceSpy = {
    currentUser: vi.fn().mockReturnValue({ firstName: 'Sarah', lastName: 'Connor' }),
    isAuthenticated: vi.fn().mockReturnValue(true)
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgentDashboard],
      providers: [
        provideRouter([]),
        { provide: AnalyticsService, useValue: analyticsServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
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
});

