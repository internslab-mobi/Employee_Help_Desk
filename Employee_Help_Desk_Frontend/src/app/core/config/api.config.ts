/**
 * Centralized API Configuration for Gateway Architecture.
 * All frontend requests route exclusively through the Auth Service Gateway at /api.
 * Downstream service URLs and ports are never exposed in frontend client code.
 */
export const API_BASE_URL = '/api';

export const API_ENDPOINTS = {
  auth: {
    login: `${API_BASE_URL}/user/login`,
    logout: `${API_BASE_URL}/user/logout`,
    refreshToken: `${API_BASE_URL}/user/refresh-token`,
    changePassword: `${API_BASE_URL}/user/change-password`,
    forgotPasswordRequestOtp: `${API_BASE_URL}/user/forgot-password/request-otp`,
    forgotPasswordVerifyOtp: `${API_BASE_URL}/user/forgot-password/verify-otp`,
    forgotPasswordResetPassword: `${API_BASE_URL}/user/forgot-password/reset-password`
  },
  user: {
    base: `${API_BASE_URL}/helpdesk/user`,
    profileMe: `${API_BASE_URL}/helpdesk/user/me`,
    profile: (id: number) => `${API_BASE_URL}/helpdesk/user/${id}`,
    search: `${API_BASE_URL}/helpdesk/user/search`
  },
  analytics: {
    base: `${API_BASE_URL}/helpdesk/analytics`,
    dashboard: `${API_BASE_URL}/analytics/dashboard`
  },
  reports: {
    base: `${API_BASE_URL}/helpdesk/reports`,
    summaryExport: `${API_BASE_URL}/reports/summary/export`
  },
  tickets: {
    base: `${API_BASE_URL}/helpdesk/tickets`
  },
  notifications: {
    base: `${API_BASE_URL}/helpdesk/notifications`
  }
} as const;

