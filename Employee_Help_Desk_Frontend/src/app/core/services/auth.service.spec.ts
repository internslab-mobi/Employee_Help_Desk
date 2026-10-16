import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
  AuthService,
  ACCESS_TOKEN_EXPIRATION_MS,
  TOKEN_REFRESH_SCHEDULE_MS,
  MAX_REFRESH_ATTEMPTS,
  MAX_SESSION_DURATION_MS
} from './auth.service';
import { API_ENDPOINTS } from '../config/api.config';
import { LoginResponse, TokenRefreshResponse } from '../models/auth.models';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: Router;

  const mockLoginResponse: LoginResponse = {
    accessToken: 'test-jwt-access-token',
    refreshToken: 'test-refresh-token',
    tokenType: 'Bearer',
    expiresIn: ACCESS_TOKEN_EXPIRATION_MS,
    employeeId: 42,
    role: 'EMPLOYEE'
  };

  const mockRefreshResponse: TokenRefreshResponse = {
    accessToken: 'new-jwt-access-token',
    refreshToken: 'test-refresh-token',
    tokenType: 'Bearer',
    expiresIn: ACCESS_TOKEN_EXPIRATION_MS
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');

    service = TestBed.inject(AuthService);
  });

  afterEach(() => {
    service.logout();
    httpMock.verify();
    localStorage.clear();
    vi.useRealTimers();
  });

  it('should have correct timing constants for 15-min token, 14-min refresh, and 45-min session', () => {
    expect(ACCESS_TOKEN_EXPIRATION_MS).toBe(15 * 60 * 1000);
    expect(TOKEN_REFRESH_SCHEDULE_MS).toBe(14 * 60 * 1000);
    expect(MAX_REFRESH_ATTEMPTS).toBe(3);
    expect(MAX_SESSION_DURATION_MS).toBe(45 * 60 * 1000);
  });

  it('should save session, initialize refresh count to 0, and record session start on login', () => {
    service.login({ email: 'user@example.com', password: 'password123' }).subscribe();

    const req = httpMock.expectOne(API_ENDPOINTS.auth.login);
    req.flush(mockLoginResponse);

    // Profile request triggered after login
    const profileReq = httpMock.expectOne(API_ENDPOINTS.user.profile(42));
    profileReq.flush({ id: 42, firstName: 'John', lastName: 'Doe', email: 'user@example.com', role: 'EMPLOYEE', department: 'IT' });

    expect(service.getToken()).toBe('test-jwt-access-token');
    expect(service.getRefreshToken()).toBe('test-refresh-token');
    expect(service.getRole()).toBe('EMPLOYEE');
    expect(service.getRefreshCount()).toBe(0);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should trigger proactive refresh at the 14th minute and increment refresh count', () => {
    vi.useFakeTimers();

    service.login({ email: 'user@example.com', password: 'password123' }).subscribe();
    const loginReq = httpMock.expectOne(API_ENDPOINTS.auth.login);
    loginReq.flush(mockLoginResponse);

    const profileReq = httpMock.expectOne(API_ENDPOINTS.user.profile(42));
    profileReq.flush({});

    expect(service.getRefreshCount()).toBe(0);

    // Advance to 14th minute (14 * 60 * 1000 ms)
    vi.advanceTimersByTime(TOKEN_REFRESH_SCHEDULE_MS);

    // Proactive refresh HTTP request should be sent
    const refreshReq = httpMock.expectOne(API_ENDPOINTS.auth.refreshToken);
    expect(refreshReq.request.body).toEqual({ refreshToken: 'test-refresh-token' });
    refreshReq.flush(mockRefreshResponse);

    expect(service.getToken()).toBe('new-jwt-access-token');
    expect(service.getRefreshCount()).toBe(1);
  });

  it('should allow up to 3 refreshes and stop further refreshes once limit is reached', () => {
    vi.useFakeTimers();

    service.login({ email: 'user@example.com', password: 'password123' }).subscribe();
    httpMock.expectOne(API_ENDPOINTS.auth.login).flush(mockLoginResponse);
    httpMock.expectOne(API_ENDPOINTS.user.profile(42)).flush({});

    // Refresh 1 at 14m
    vi.advanceTimersByTime(TOKEN_REFRESH_SCHEDULE_MS);
    httpMock.expectOne(API_ENDPOINTS.auth.refreshToken).flush({
      ...mockRefreshResponse,
      accessToken: 'token-cycle-1'
    });
    expect(service.getRefreshCount()).toBe(1);

    // Refresh 2 at 28m
    vi.advanceTimersByTime(TOKEN_REFRESH_SCHEDULE_MS);
    httpMock.expectOne(API_ENDPOINTS.auth.refreshToken).flush({
      ...mockRefreshResponse,
      accessToken: 'token-cycle-2'
    });
    expect(service.getRefreshCount()).toBe(2);

    // Refresh 3 at 42m
    vi.advanceTimersByTime(TOKEN_REFRESH_SCHEDULE_MS);
    httpMock.expectOne(API_ENDPOINTS.auth.refreshToken).flush({
      ...mockRefreshResponse,
      accessToken: 'token-cycle-3'
    });
    expect(service.getRefreshCount()).toBe(3);

    // Attempting a 4th refresh should fail and logout
    let refreshError: any = null;
    service.refreshToken().subscribe({
      next: () => {},
      error: (err) => {
        refreshError = err;
      }
    });

    expect(refreshError).toBeTruthy();
    expect(refreshError.message).toContain('Maximum refresh count exceeded');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should automatically logout user when 45 minutes total session completes', () => {
    vi.useFakeTimers();

    service.login({ email: 'user@example.com', password: 'password123' }).subscribe();
    httpMock.expectOne(API_ENDPOINTS.auth.login).flush(mockLoginResponse);
    httpMock.expectOne(API_ENDPOINTS.user.profile(42)).flush({});

    // Refresh 1 at 14m
    vi.advanceTimersByTime(TOKEN_REFRESH_SCHEDULE_MS);
    httpMock.expectOne(API_ENDPOINTS.auth.refreshToken).flush(mockRefreshResponse);

    // Refresh 2 at 28m
    vi.advanceTimersByTime(TOKEN_REFRESH_SCHEDULE_MS);
    httpMock.expectOne(API_ENDPOINTS.auth.refreshToken).flush(mockRefreshResponse);

    // Refresh 3 at 42m
    vi.advanceTimersByTime(TOKEN_REFRESH_SCHEDULE_MS);
    httpMock.expectOne(API_ENDPOINTS.auth.refreshToken).flush(mockRefreshResponse);

    // Advance remaining 3 minutes to reach exactly 45 minutes
    vi.advanceTimersByTime(3 * 60 * 1000);

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(service.isAuthenticated()).toBe(false);
  });
});
