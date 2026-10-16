import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, tap, throwError } from 'rxjs';
import { API_ENDPOINTS } from '../config/api.config';
import { EmployeeProfile, LoginRequest, LoginResponse, TokenRefreshResponse, UserRole } from '../models/auth.models';

const TOKEN_KEY = 'ehd_access_token';
const REFRESH_TOKEN_KEY = 'ehd_refresh_token';
const EMPLOYEE_ID_KEY = 'ehd_employee_id';
const ROLE_KEY = 'ehd_role';
const PROFILE_KEY = 'ehd_profile';
const REFRESH_COUNT_KEY = 'ehd_refresh_count';
const SESSION_START_KEY = 'ehd_session_start';

export const ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000; // 15 minutes (900,000 ms)
export const TOKEN_REFRESH_SCHEDULE_MS = 14 * 60 * 1000; // 14th minute completed (840,000 ms)
export const MAX_REFRESH_ATTEMPTS = 3; // Client can generate 3 new access tokens using refresh token
export const MAX_SESSION_DURATION_MS = 45 * 60 * 1000; // Totally client can stay for 45 minutes (2,700,000 ms)

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  isRefreshing = false;
  readonly refreshTokenSubject = new BehaviorSubject<string | null>(null);

  readonly token = signal<string | null>(this.getStoredToken());
  readonly role = signal<UserRole | null>(this.getStoredRole());
  readonly currentUser = signal<EmployeeProfile | null>(this.getStoredProfile());
  readonly isLoggedIn = computed(() => !!this.token());

  private proactiveRefreshTimer: ReturnType<typeof setTimeout> | null = null;
  private sessionTimeoutTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    if (this.isLoggedIn()) {
      const sessionStart = this.getSessionStartTime();
      const elapsed = Date.now() - sessionStart;
      if (elapsed >= MAX_SESSION_DURATION_MS) {
        this.logout();
      } else {
        this.scheduleSessionExpiration();
        const currentToken = this.getToken();
        if (currentToken) {
          this.scheduleTokenRefresh(currentToken);
        }
      }
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(API_ENDPOINTS.auth.login, credentials).pipe(
      tap((res) => {
        this.saveAuthSession(res);
        this.loadProfile(res.employeeId).subscribe({
          next: () => {},
          error: () => {}
        });
      })
    );
  }

  loadProfile(employeeId: number): Observable<EmployeeProfile> {
    return this.http.get<EmployeeProfile>(API_ENDPOINTS.user.profile(employeeId)).pipe(
      tap((profile) => {
        this.currentUser.set(profile);
        localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
      })
    );
  }

  logout(): void {
    this.clearAllTimers();

    this.isRefreshing = false;
    this.refreshTokenSubject.next(null);

    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(EMPLOYEE_ID_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(PROFILE_KEY);
    localStorage.removeItem(REFRESH_COUNT_KEY);
    localStorage.removeItem(SESSION_START_KEY);

    this.token.set(null);
    this.role.set(null);
    this.currentUser.set(null);

    this.router.navigate(['/login']);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  refreshToken(): Observable<TokenRefreshResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.logout();
      return throwError(() => new Error('No refresh token available'));
    }

    const currentCount = this.getRefreshCount();
    if (currentCount >= MAX_REFRESH_ATTEMPTS) {
      this.logout();
      return throwError(() => new Error('Maximum refresh count exceeded (3/3)'));
    }

    const sessionStart = this.getSessionStartTime();
    if (Date.now() - sessionStart >= MAX_SESSION_DURATION_MS) {
      this.logout();
      return throwError(() => new Error('Maximum session duration of 45 minutes exceeded'));
    }

    return this.http
      .post<TokenRefreshResponse>(API_ENDPOINTS.auth.refreshToken, { refreshToken })
      .pipe(
        tap((res) => {
          localStorage.setItem(TOKEN_KEY, res.accessToken);
          if (res.refreshToken) {
            localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken);
          }
          this.token.set(res.accessToken);

          // Increment refresh count (up to 3 times)
          const newCount = currentCount + 1;
          this.setRefreshCount(newCount);

          // If still under 3 refreshes, schedule next 14th minute refresh
          if (newCount < MAX_REFRESH_ATTEMPTS) {
            this.scheduleTokenRefresh(res.accessToken);
          } else {
            this.clearRefreshTimer();
          }
        }),
        catchError((err) => {
          this.logout();
          return throwError(() => err);
        })
      );
  }

  scheduleTokenRefresh(token?: string): void {
    this.clearRefreshTimer();

    const currentRefreshCount = this.getRefreshCount();
    if (currentRefreshCount >= MAX_REFRESH_ATTEMPTS) {
      return;
    }

    const sessionStart = this.getSessionStartTime();
    const elapsedSession = Date.now() - sessionStart;
    if (elapsedSession >= MAX_SESSION_DURATION_MS) {
      this.logout();
      return;
    }

    let delay = TOKEN_REFRESH_SCHEDULE_MS;

    const tokenToCheck = token || this.getToken();
    if (tokenToCheck) {
      const expTime = this.getTokenExpiration(tokenToCheck);
      if (expTime) {
        // Target: 14th minute completed = 1 minute (60,000 ms) before expiration
        const targetRefreshTime = expTime - 60_000;
        delay = Math.max(0, targetRefreshTime - Date.now());
      }
    }

    const remainingSessionTime = MAX_SESSION_DURATION_MS - elapsedSession;
    if (delay > remainingSessionTime) {
      return;
    }

    this.proactiveRefreshTimer = setTimeout(() => {
      this.executeProactiveRefresh();
    }, delay);
  }

  private executeProactiveRefresh(): void {
    if (!this.isAuthenticated()) {
      return;
    }

    const currentCount = this.getRefreshCount();
    if (currentCount >= MAX_REFRESH_ATTEMPTS) {
      return;
    }

    const sessionStart = this.getSessionStartTime();
    if (Date.now() - sessionStart >= MAX_SESSION_DURATION_MS) {
      this.logout();
      return;
    }

    this.refreshToken().subscribe({
      next: () => {},
      error: () => {}
    });
  }

  private scheduleSessionExpiration(): void {
    this.clearSessionTimer();
    const sessionStart = this.getSessionStartTime();
    const elapsed = Date.now() - sessionStart;
    const remainingTime = Math.max(0, MAX_SESSION_DURATION_MS - elapsed);

    this.sessionTimeoutTimer = setTimeout(() => {
      this.logout();
    }, remainingTime);
  }

  getToken(): string | null {
    return this.token();
  }

  getRole(): UserRole | null {
    return this.role();
  }

  getEmployeeId(): number | null {
    const idStr = localStorage.getItem(EMPLOYEE_ID_KEY);
    return idStr ? parseInt(idStr, 10) : null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getRefreshCount(): number {
    const raw = localStorage.getItem(REFRESH_COUNT_KEY);
    return raw ? parseInt(raw, 10) : 0;
  }

  private setRefreshCount(count: number): void {
    localStorage.setItem(REFRESH_COUNT_KEY, String(count));
  }

  getSessionStartTime(): number {
    const raw = localStorage.getItem(SESSION_START_KEY);
    return raw ? parseInt(raw, 10) : Date.now();
  }

  private setSessionStartTime(timestamp: number): void {
    localStorage.setItem(SESSION_START_KEY, String(timestamp));
  }

  private getTokenExpiration(token: string): number | null {
    try {
      const parts = token.split('.');
      if (parts.length < 2) return null;
      const payload = JSON.parse(atob(parts[1]));
      return payload.exp ? payload.exp * 1000 : null;
    } catch {
      return null;
    }
  }

  private clearRefreshTimer(): void {
    if (this.proactiveRefreshTimer) {
      clearTimeout(this.proactiveRefreshTimer);
      this.proactiveRefreshTimer = null;
    }
  }

  private clearSessionTimer(): void {
    if (this.sessionTimeoutTimer) {
      clearTimeout(this.sessionTimeoutTimer);
      this.sessionTimeoutTimer = null;
    }
  }

  private clearAllTimers(): void {
    this.clearRefreshTimer();
    this.clearSessionTimer();
  }

  private saveAuthSession(res: LoginResponse): void {
    localStorage.setItem(TOKEN_KEY, res.accessToken);
    if (res.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken);
    }
    localStorage.setItem(EMPLOYEE_ID_KEY, String(res.employeeId));
    localStorage.setItem(ROLE_KEY, res.role);

    this.token.set(res.accessToken);
    this.role.set(res.role);

    this.setRefreshCount(0);
    this.setSessionStartTime(Date.now());

    this.scheduleSessionExpiration();
    this.scheduleTokenRefresh(res.accessToken);
  }

  private getStoredToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private getStoredRole(): UserRole | null {
    return localStorage.getItem(ROLE_KEY) as UserRole | null;
  }

  private getStoredProfile(): EmployeeProfile | null {
    const raw = localStorage.getItem(PROFILE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as EmployeeProfile;
    } catch {
      return null;
    }
  }
}
