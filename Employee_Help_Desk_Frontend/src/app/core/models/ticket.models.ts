export interface IdName {
  id: number;
  name: string;
}

export type TicketStatusType =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'PENDING'
  | 'RESOLVED'
  | 'WITHDRAWN'
  | 'REOPENED';

export type TicketPriorityType = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type SlaStatusType =
  | 'ACTIVE'
  | 'WARNING'
  | 'BREACHED'
  | 'PAUSED'
  | 'MET'
  | 'COMPLETED';

export type TicketViewType = 'CREATED' | 'ASSIGNED' | 'DEPARTMENT';

export interface TicketResponse {
  id: number;
  ticketNumber: string;
  requester?: IdName;
  department?: IdName;
  category?: IdName;
  subCategory?: IdName;
  subject: string;
  description: string;
  priority: TicketPriorityType | string;
  status: TicketStatusType | string;
  assignedAgent?: IdName;
  managerId?: number;
  reopenCount?: number;
  createdAt: string;
  updatedAt?: string;
  resolvedAt?: string;
  reopenedAt?: string;
  assignedAt?: string;
  slaStatus?: SlaStatusType | string;
  resolutionSummary?: string;
  holdReason?: string;
  withdrawalReason?: string;
  reopenReason?: string;
}

export interface CreateTicketRequest {
  departmentId: number;
  categoryId?: number;
  subCategoryId?: number;
  subject: string;
  description: string;
}

export interface TicketAttachmentResponse {
  id: number;
  originalFilename: string;
  mimeType: string;
  fileSize: number;
  attachmentType: string;
}

export interface TicketMessageResponse {
  id: number;
  ticketId: number;
  senderId: number;
  senderName: string;
  content: string;
  seen?: boolean;
  createdAt?: string;
  attachments?: TicketAttachmentResponse[];
}

export interface TicketHistoryResponse {
  id: number;
  eventType: string;
  oldValue?: string;
  newValue?: string;
  createdAt: string;
}

export interface StatusTimelineStep {
  status: string;
  label: string;
  state: 'completed' | 'current' | 'pending';
  isLineCompleted: boolean;
}

export interface TicketSlaResponse {
  id: number;
  policyName?: string;
  cycleNumber?: number;
  allocatedMinutes?: number;
  slaStartAt?: string;
  originalDeadlineAt?: string;
  currentDeadlineAt?: string;
  warningAt?: string;
  status: SlaStatusType | string;
  pausedAt?: string;
  breachedAt?: string;
}

export interface DepartmentResponse {
  id: number;
  code: string;
  name: string;
  description?: string;
  isActive?: boolean;
  timezone?: string;
}

export interface CategoryResponse {
  id: number;
  departmentId: number;
  name: string;
  description?: string;
}

export interface SubCategoryResponse {
  id: number;
  categoryId: number;
  name: string;
  description?: string;
  priority: TicketPriorityType | string;
  skillIds?: number[];
  skillNames?: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface TicketSearchParams {
  view: TicketViewType;
  employeeId?: number;
  departmentId?: number;
  agentId?: number;
  status?: string;
  priority?: string;
  fromDate?: string;
  toDate?: string;
  search?: string;
  unassigned?: boolean;
  slaStatus?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface AssignableAgentResponse {
  agentId: number;
  employeeId?: number;
  employeeCode?: string;
  firstName: string;
  lastName?: string;
  email: string;
  departmentId?: number;
  departmentName?: string;
  matchedSkillCount: number;
  requiredSkillCount: number;
  matchPercentage: number;
  activeTicketCount: number;
  lastAssignedAt?: string;
}
