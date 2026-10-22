import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Login } from './login';
import { AuthService } from '../../../../core/services/auth.service';
import { LoginResponse } from '../../../../core/models/auth.models';

describe('Login Component', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let authServiceSpy: { login: ReturnType<typeof vi.fn> };
  let router: Router;

  const mockLoginResponse: LoginResponse = {
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 900000,
    employeeId: 10,
    role: 'EMPLOYEE'
  };

  beforeEach(async () => {
    authServiceSpy = {
      login: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should create the login component with default values', () => {
    expect(component).toBeTruthy();
    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('');
    expect(component.showPassword).toBe(false);
  });

  it('should mark all fields as touched and not submit when form is invalid', () => {
    component.loginForm.setValue({ email: '', password: '' });
    component.onLogin();

    expect(component.loginForm.invalid).toBe(true);
    expect(component.isLoading).toBe(false);
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('should set isLoading to true while authenticating, and navigate to /dashboard on success', () => {
    authServiceSpy.login.mockReturnValue(of(mockLoginResponse));

    component.loginForm.setValue({
      email: 'divibalakrishnan2005@gmail.com',
      password: 'Welcome@123'
    });

    component.onLogin();

    expect(authServiceSpy.login).toHaveBeenCalledWith({
      email: 'divibalakrishnan2005@gmail.com',
      password: 'Welcome@123'
    });
    expect(component.isLoading).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.errorMessage).toBe('');
  });

  it('should reset isLoading to false and display an error message on 401 Unauthorized', () => {
    authServiceSpy.login.mockReturnValue(
      throwError(() => ({
        status: 401,
        error: { message: 'Invalid email or password' }
      }))
    );

    component.loginForm.setValue({
      email: 'divibalakrishnan2005@gmail.com',
      password: 'WrongPassword123'
    });

    component.onLogin();

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Invalid email or password');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should reset isLoading to false and display a connection error message when backend is unreachable (status 0)', () => {
    authServiceSpy.login.mockReturnValue(
      throwError(() => ({
        status: 0,
        error: new ProgressEvent('error')
      }))
    );

    component.loginForm.setValue({
      email: 'divibalakrishnan2005@gmail.com',
      password: 'Welcome@123'
    });

    component.onLogin();

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toContain('Unable to connect to the authentication server');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should toggle password visibility flag and update input type without submitting form', () => {
    const toggleBtn: HTMLButtonElement = fixture.nativeElement.querySelector('#login-password-toggle');
    const pwdInput: HTMLInputElement = fixture.nativeElement.querySelector('#password');

    expect(toggleBtn).toBeTruthy();
    expect(toggleBtn.type).toBe('button');
    expect(toggleBtn.getAttribute('aria-label')).toBe('Show password');
    expect(pwdInput.type).toBe('password');

    toggleBtn.click();
    fixture.detectChanges();

    expect(component.showPassword).toBe(true);
    expect(pwdInput.type).toBe('text');
    expect(toggleBtn.getAttribute('aria-label')).toBe('Hide password');
    expect(authServiceSpy.login).not.toHaveBeenCalled();

    toggleBtn.click();
    fixture.detectChanges();

    expect(component.showPassword).toBe(false);
    expect(pwdInput.type).toBe('password');
    expect(toggleBtn.getAttribute('aria-label')).toBe('Show password');
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('should display raw string error message returned directly from backend', () => {
    authServiceSpy.login.mockReturnValue(
      throwError(() => ({
        status: 403,
        error: 'Your account is deactivated. Please contact support.'
      }))
    );

    component.loginForm.setValue({
      email: 'divibalakrishnan2005@gmail.com',
      password: 'SomePassword123'
    });

    component.onLogin();

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Your account is deactivated. Please contact support.');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should display error field when backend returns { error: "..." }', () => {
    authServiceSpy.login.mockReturnValue(
      throwError(() => ({
        status: 400,
        error: { error: 'Bad credentials' }
      }))
    );

    component.loginForm.setValue({
      email: 'divibalakrishnan2005@gmail.com',
      password: 'SomePassword123'
    });

    component.onLogin();

    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toBe('Bad credentials');
  });
});
