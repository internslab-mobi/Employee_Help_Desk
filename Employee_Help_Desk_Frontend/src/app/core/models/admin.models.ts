import { TicketPriorityType } from './ticket.models';
import { UserRole } from './auth.models';

export interface CreateDepartmentRequest {
  code: string;
  name: string;
  description?: string;
  timezone?: string;
}

export interface CreateCategoryRequest {
  departmentId: number;
  name: string;
  description?: string;
}

export interface CreateSubCategoryRequest {
  categoryId: number;
  name: string;
  description?: string;
  priority?: TicketPriorityType | string;
  skillIds?: number[];
}

export interface SkillResponse {
  id: number;
  name: string;
  description?: string;
  isActive: boolean;
}

export interface CreateSkillRequest {
  name: string;
  description?: string;
}

export interface AssignSkillsRequest {
  skillIds: number[];
}

export interface SlaPolicyResponse {
  id: number;
  departmentId: number;
  departmentName?: string;
  subCategoryId: number;
  subCategoryName?: string;
  durationMinutes: number;
  warningMinutes?: number;
  isActive: boolean;
}

export interface CreateSlaPolicyRequest {
  departmentId: number;
  subCategoryId: number;
  durationMinutes: number;
  warningMinutes?: number;
}

export interface HolidayResponse {
  id: number;
  departmentId: number;
  departmentName?: string;
  holidayDate: string;
  holidayName: string;
}

export interface CreateHolidayRequest {
  departmentId: number;
  holidayDate: string;
  holidayName: string;
}

export interface CreateUserRequest {
  firstName: string;
  lastName?: string;
  email: string;
  phone?: string;
  designation?: string;
  departmentId?: number | null;
  managerId?: number | null;
  role: UserRole;
  dateOfJoining?: string;
  timezone?: string;
}

export interface EmployeeResponse {
  id: number;
  employeeCode?: string;
  firstName: string;
  lastName?: string;
  email: string;
  phone?: string;
  designation?: string;
  departmentId?: number;
  departmentName?: string;
  managerId?: number;
  managerName?: string;
  role: UserRole;
  employmentStatus?: string;
  dateOfJoining?: string;
  timezone?: string;
  profilePhotoUrl?: string;
}
