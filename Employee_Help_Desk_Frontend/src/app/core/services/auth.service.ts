import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EmployeeProfile, LoginRequest, LoginResponse, UserRole } from '../models/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly apiUrl = environment.backendUrl || 'http://localhost:8082';

  // Signals
  readonly currentUser = signal<EmployeeProfile | null>(null);
  readonly currentRole = signal<UserRole | null>(null);
  readonly token = signal<string | null>(null);

  constructor() {
    this.restoreSession();
  }

  private restoreSession(): void {
    const savedToken = localStorage.getItem('access_token');
    const savedRole = localStorage.getItem('user_role') as UserRole | null;
    const savedProfileStr = localStorage.getItem('user_profile');
    const savedEmployeeId = localStorage.getItem('employee_id');

    if (savedToken) {
      this.token.set(savedToken);
      if (savedRole) {
        this.currentRole.set(savedRole);
      }

      if (savedProfileStr) {
        try {
          const profile = JSON.parse(savedProfileStr) as EmployeeProfile;
          this.currentUser.set(profile);
        } catch (e) {
          console.warn('Failed to parse cached user profile', e);
        }
      } else if (savedEmployeeId) {
        // Fetch profile asynchronously to restore full details
        const empId = Number(savedEmployeeId);
        if (!isNaN(empId)) {
          this.loadProfile(empId).subscribe({
            error: () => {}
          });
        }
      }
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/user/login`, credentials).pipe(
      tap((res) => {
        if (res.accessToken) {
          localStorage.setItem('access_token', res.accessToken);
          this.token.set(res.accessToken);
        }
        if (res.refreshToken) {
          localStorage.setItem('refresh_token', res.refreshToken);
        }
        if (res.role) {
          localStorage.setItem('user_role', res.role);
          this.currentRole.set(res.role);
        }
        if (res.employeeId != null) {
          localStorage.setItem('employee_id', res.employeeId.toString());
          // Fetch full employee profile
          this.loadProfile(res.employeeId).subscribe({
            error: (err) => console.warn('Could not load employee profile after login:', err)
          });
        }
      })
    );
  }

  loadProfile(employeeId: number): Observable<EmployeeProfile> {
    return this.http.get<EmployeeProfile>(`${this.apiUrl}/user/${employeeId}`).pipe(
      tap((profile) => {
        this.currentUser.set(profile);
        localStorage.setItem('user_profile', JSON.stringify(profile));
        if (profile.role) {
          this.currentRole.set(profile.role);
          localStorage.setItem('user_role', profile.role);
        }
      })
    );
  }

  getToken(): string | null {
    return this.token() || localStorage.getItem('access_token');
  }

  getRole(): UserRole | null {
    return this.currentRole() || (localStorage.getItem('user_role') as UserRole | null);
  }

  getEmployeeId(): number | null {
    const profile = this.currentUser();
    if (profile && profile.id != null) {
      return profile.id;
    }
    const saved = localStorage.getItem('employee_id');
    return saved ? Number(saved) : null;
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return !!token && token.trim().length > 0;
  }

  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user_role');
    localStorage.removeItem('employee_id');
    localStorage.removeItem('user_profile');

    this.token.set(null);
    this.currentRole.set(null);
    this.currentUser.set(null);

    this.router.navigate(['/login']);
  }
}