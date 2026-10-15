export type ReportType =
  | 'GENERAL_SUMMARY'
  | 'TICKET_SUMMARY'
  | 'USER_SUMMARY'
  | 'EMPLOYEE_SUMMARY'
  | 'AGENT_SUMMARY'
  | 'MANAGER_SUMMARY'
  | 'ADMIN_SUMMARY'
  | 'SLA_SUMMARY';

export type ReportFormat = 'JSON' | 'EXCEL';

export interface ReportSummaryRequest {
  reportType?: ReportType;
  format?: ReportFormat;
  fromDate?: string;
  toDate?: string;
  departmentId?: number;
  status?: string;
  priority?: string;
  employeeId?: number;
}

export interface ReportSummaryResponse {
  reportType: ReportType;
  format: ReportFormat;
  generatedAt: string;
  role: string;
  employeeId: number;
  employeeName: string;
  departmentName?: string;
  timezone?: string;
  fromDate?: string;
  toDate?: string;
  totalRecords: number;
  summary?: any;
  statusDistribution?: any[];
  priorityDistribution?: any[];
  categoryDistribution?: any[];
  departmentDistribution?: any[];
  agentWorkload?: any[];
  employeeStats?: any;
  trends?: any[];
  sampleRecords?: any[];
  metrics?: Record<string, any>;
}
