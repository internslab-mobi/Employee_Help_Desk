import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../services/auth.service';

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: { getToken: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authServiceSpy = {
      getToken: vi.fn(),
      logout: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should not add Authorization header to login endpoint even if token exists', () => {
    authServiceSpy.getToken.mockReturnValue('stale-token-xyz');

    http.post('http://localhost:8082/user/login', { email: 'test@example.com', password: 'pwd' }).subscribe();

    const req = httpMock.expectOne('http://localhost:8082/user/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should add Authorization header to protected endpoint when token exists', () => {
    authServiceSpy.getToken.mockReturnValue('valid-jwt-token');

    http.get('http://localhost:8082/analytics/dashboard').subscribe();

    const req = httpMock.expectOne('http://localhost:8082/analytics/dashboard');
    expect(req.request.headers.get('Authorization')).toBe('Bearer valid-jwt-token');
    req.flush({});
  });

  it('should not add Authorization header if no token is available', () => {
    authServiceSpy.getToken.mockReturnValue(null);

    http.get('http://localhost:8082/analytics/dashboard').subscribe();

    const req = httpMock.expectOne('http://localhost:8082/analytics/dashboard');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('should call authService.logout when protected endpoint returns 401 and no refresh token exists', () => {
    authServiceSpy.getToken.mockReturnValue('expired-token');

    http.get('http://localhost:8082/user/10').subscribe({
      next: () => {},
      error: () => {}
    });

    const req = httpMock.expectOne('http://localhost:8082/user/10');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.logout).toHaveBeenCalled();
  });

  it('should refresh token and retry the original request when protected endpoint returns 401 and refresh token exists', () => {
    authServiceSpy.getToken.mockReturnValue('expired-token');
    (authServiceSpy as any).getRefreshToken = vi.fn().mockReturnValue('valid-refresh-token');
    (authServiceSpy as any).isRefreshing = false;
    (authServiceSpy as any).refreshTokenSubject = new BehaviorSubject<string | null>(null);
    (authServiceSpy as any).refreshToken = vi.fn().mockReturnValue(of({
      accessToken: 'new-refreshed-token',
      refreshToken: 'new-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 3600
    }));

    let responseData: any;
    http.get('http://localhost:8082/user/10').subscribe(data => {
      responseData = data;
    });

    // Initial request fails with 401
    const req1 = httpMock.expectOne('http://localhost:8082/user/10');
    expect(req1.request.headers.get('Authorization')).toBe('Bearer expired-token');
    req1.flush({ message: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

    expect((authServiceSpy as any).refreshToken).toHaveBeenCalled();

    // Retried request with new access token
    const req2 = httpMock.expectOne('http://localhost:8082/user/10');
    expect(req2.request.headers.get('Authorization')).toBe('Bearer new-refreshed-token');
    req2.flush({ id: 10, name: 'Alice' });

    expect(responseData).toEqual({ id: 10, name: 'Alice' });
    expect(authServiceSpy.logout).not.toHaveBeenCalled();
  });

  it('should logout if refresh token call fails', () => {
    authServiceSpy.getToken.mockReturnValue('expired-token');
    (authServiceSpy as any).getRefreshToken = vi.fn().mockReturnValue('invalid-refresh-token');
    (authServiceSpy as any).isRefreshing = false;
    (authServiceSpy as any).refreshTokenSubject = new BehaviorSubject<string | null>(null);
    (authServiceSpy as any).refreshToken = vi.fn().mockReturnValue(throwError(() => new Error('Refresh failed')));

    http.get('http://localhost:8082/user/10').subscribe({
      next: () => {},
      error: () => {}
    });

    const req1 = httpMock.expectOne('http://localhost:8082/user/10');
    req1.flush({ message: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

    expect((authServiceSpy as any).refreshToken).toHaveBeenCalled();
    expect(authServiceSpy.logout).toHaveBeenCalled();
  });
});
