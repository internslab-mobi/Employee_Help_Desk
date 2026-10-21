import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_ENDPOINTS } from '../config/api.config';
<<<<<<< HEAD
import { EmployeeProfile, LoginRequest, LoginResponse, UserRole } from '../models/auth.models';
=======
import { EmployeeProfile, LoginRequest, LoginResponse, TokenRefreshResponse, UpdateProfileRequest, UserRole } from '../models/auth.models';
>>>>>>> 14450b4c (Fixed profile page)

const TOKEN_KEY = 'ehd_access_token';
const REFRESH_TOKEN_KEY = 'ehd_refresh_token';
const EMPLOYEE_ID_KEY = 'ehd_employee_id';
const ROLE_KEY = 'ehd_role';
const PROFILE_KEY = 'ehd_profile';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  readonly token = signal<string | null>(this.getStoredToken());
  readonly role = signal<UserRole | null>(this.getStoredRole());
  readonly currentUser = signal<EmployeeProfile | null>(this.getStoredProfile());
  readonly isLoggedIn = computed(() => !!this.token());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

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

  getProfile(): Observable<EmployeeProfile> {
    const employeeId = this.getEmployeeId();
    if (employeeId) {
      return this.loadProfile(employeeId);
    }
    return this.http.get<EmployeeProfile>(API_ENDPOINTS.user.profileMe).pipe(
      tap((profile) => {
        this.currentUser.set(profile);
        localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
      })
    );
  }

  updateProfile(request: UpdateProfileRequest, profileImage?: File): Observable<EmployeeProfile> {
    const employeeId = this.getEmployeeId();
    const url = employeeId ? API_ENDPOINTS.user.profile(employeeId) : API_ENDPOINTS.user.profileMe;

    const formData = new FormData();
    const jsonBlob = new Blob([JSON.stringify(request)], { type: 'application/json' });
    formData.append('request', jsonBlob);

    if (profileImage) {
      formData.append('profileImage', profileImage, profileImage.name);
    }

    return this.http.patch<EmployeeProfile>(url, formData).pipe(
      tap((updated) => {
        this.currentUser.set(updated);
        localStorage.setItem(PROFILE_KEY, JSON.stringify(updated));
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(EMPLOYEE_ID_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(PROFILE_KEY);

    this.token.set(null);
    this.role.set(null);
    this.currentUser.set(null);

    this.router.navigate(['/login']);
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

  private saveAuthSession(res: LoginResponse): void {
    localStorage.setItem(TOKEN_KEY, res.accessToken);
    if (res.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken);
    }
    localStorage.setItem(EMPLOYEE_ID_KEY, String(res.employeeId));
    localStorage.setItem(ROLE_KEY, res.role);

    this.token.set(res.accessToken);
    this.role.set(res.role);
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
