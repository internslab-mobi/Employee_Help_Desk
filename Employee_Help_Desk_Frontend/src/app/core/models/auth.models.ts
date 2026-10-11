export type UserRole = 'EMPLOYEE' | 'AGENT' | 'MANAGER' | 'ADMIN';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresIn: number;
  employeeId: number;
  role: UserRole;
}

export interface EmployeeProfile {
  id: number;
  employeeCode?: string;
  firstName: string;
  lastName?: string;
  email: string;
  phone?: string;
  designation?: string;
  departmentId?: number;
  departmentName?: string;
  employmentStatus: string;
  role: UserRole;
  timezone?: string;
}
