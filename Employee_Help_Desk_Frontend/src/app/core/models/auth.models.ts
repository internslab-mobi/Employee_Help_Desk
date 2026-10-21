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

<<<<<<< HEAD
export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresIn: number;
}

=======
>>>>>>> 22515536 (microservices summary report)
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
  dateOfJoining?: string;
  dateOfExit?: string;
  timezone?: string;
  createdAt?: string;
  updatedAt?: string;
  profileImageUrl?: string;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName?: string;
  phone?: string;
  designation?: string;
}
<<<<<<< HEAD

export interface ForgotPasswordRequest {
  email: string;
}

export interface VerifyOtpRequest {
  email: string;
  otp: string;
}

export interface VerifyOtpResponse {
  resetToken: string;
}

export interface ResetPasswordRequest {
  email: string;
  resetToken: string;
  newPassword: string;
  confirmNewPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

export interface AuthApiResponse {
  message: string;
}
=======
>>>>>>> 22515536 (microservices summary report)
