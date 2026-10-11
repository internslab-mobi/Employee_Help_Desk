import { UserRole } from './auth.models';

export interface DashboardSummary {
  totalTickets: number;
  openTickets: number;
  inProgressTickets: number;
  onHoldTickets: number;
  resolvedTickets: number;
  withdrawnTickets: number;
  reopenedTickets: number;
  overdueTickets: number;
  slaBreachedTickets: number;
  slaWarningTickets: number;
  slaCompliantTickets: number;
  slaComplianceRate: number;
  avgResolutionHours: number;
  assignedTickets: number;
  unassignedTickets: number;
}

export interface DistributionItem {
  key: string;
  label: string;
  count: number;
  percentage: number;
}

export interface TrendItem {
  period: string;
  createdCount: number;
  resolvedCount: number;
}

export interface DepartmentMetric {
  departmentId: number;
  departmentName: string;
  totalTickets: number;
  openTickets: number;
  resolvedTickets: number;
}

export interface AgentWorkload {
  agentId: number;
  employeeId?: number;
  agentName: string;
  email: string;
  departmentName: string;
  activeTickets: number;
  resolvedTickets: number;
  totalTickets: number;
}

export interface EmployeeStats {
  totalEmployees: number;
  activeEmployees: number;
  inactiveEmployees: number;
  onLeaveEmployees: number;
}

export interface RecentTicket {
  id: number;
  ticketNumber: string;
  subject: string;
  categoryName: string;
  subCategoryName: string;
  status: string;
  priority: string;
  slaStatus: string;
  requesterName: string;
  assignedAgentName: string;
  departmentName: string;
  createdAt: string;
  resolvedAt?: string;
  currentDeadlineAt?: string;
}

export interface DashboardAnalyticsResponse {
  role: UserRole;
  employeeId: number;
  employeeName: string;
  employeeEmail: string;
  designation?: string;
  departmentName?: string;
  departmentId?: number;
  summary: DashboardSummary;
  statusDistribution: DistributionItem[];
  priorityDistribution: DistributionItem[];
  departmentDistribution: DepartmentMetric[];
  categoryDistribution: DistributionItem[];
  trends: TrendItem[];
  agentWorkload: AgentWorkload[];
  employeeStats?: EmployeeStats;
  recentTickets: RecentTicket[];
  generatedAt: string;
  fromDate?: string;
  toDate?: string;
}
