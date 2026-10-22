import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ProfileComponent } from './profile';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeProfile, UpdateProfileRequest, UserRole } from '../../core/models/auth.models';

describe('ProfileComponent', () => {
  let component: ComponentFixture<ProfileComponent>['componentInstance'];
  let fixture: ComponentFixture<ProfileComponent>;
  let authServiceSpy: any;
  let routerSpy: any;

  const mockProfile: EmployeeProfile = {
    id: 1,
    employeeCode: 'EMP001',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@company.com',
    phone: '9876543210',
    designation: 'Software Engineer',
    departmentId: 1,
    departmentName: 'Information Technology',
    employmentStatus: 'ACTIVE',
    role: 'EMPLOYEE',
    dateOfJoining: '2023-01-15',
    timezone: 'Asia/Kolkata',
    createdAt: '2023-01-15T09:00:00Z',
    updatedAt: '2023-01-15T09:00:00Z'
  };

  beforeEach(async () => {
    TestBed.resetTestingModule();
    authServiceSpy = {
      getProfile: vi.fn().mockReturnValue(of(mockProfile)),
      updateProfile: vi.fn().mockReturnValue(of(mockProfile)),
      getRole: vi.fn().mockReturnValue('EMPLOYEE'),
      userRole: vi.fn().mockReturnValue('EMPLOYEE'),
      role: signal<UserRole | null>('EMPLOYEE'),
      profilePhotoUrl: signal<string | null>(null),
      logout: vi.fn(),
      changePassword: vi.fn().mockReturnValue(of({ message: 'Password changed successfully' }))
    };
    routerSpy = {
      navigate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('should create and automatically fetch user profile on init', () => {
    expect(component).toBeTruthy();
    expect(authServiceSpy.getProfile).toHaveBeenCalled();
    expect(component.profile()).toEqual(mockProfile);
    expect(component.isLoading()).toBe(false);
  });

  it('should auto-populate form fields with database profile values', () => {
    expect(component.profileForm.get('firstName')?.value).toBe('John');
    expect(component.profileForm.get('lastName')?.value).toBe('Doe');
    expect(component.profileForm.get('phone')?.value).toBe('9876543210');
    expect(component.profileForm.get('designation')?.value).toBe('Software Engineer');
  });

  it('should compute display user name and role correctly', () => {
    expect(component.displayUserName()).toBe('John Doe');
    expect(component.displayUserRole()).toBe('Employee');
  });

  it('should validate that first name is required', () => {
    const firstNameControl = component.profileForm.get('firstName');
    firstNameControl?.setValue('');
    expect(firstNameControl?.valid).toBe(false);
    expect(firstNameControl?.hasError('required')).toBe(true);
  });

  it('should enter and cancel edit mode restoring initial values', () => {
    component.enterEditMode();
    expect(component.isEditMode()).toBe(true);

    component.profileForm.get('firstName')?.setValue('ChangedName');
    component.cancelEdit();

    expect(component.isEditMode()).toBe(false);
    expect(component.profileForm.get('firstName')?.value).toBe('John');
  });

  it('should submit PATCH update with only allowed editable fields', () => {
    const updatedProfile: EmployeeProfile = {
      ...mockProfile,
      firstName: 'Johnny',
      lastName: 'Doe',
      phone: '9988776655',
      designation: 'Senior Engineer'
    };

    authServiceSpy.updateProfile.mockReturnValue(of(updatedProfile));

    component.enterEditMode();
    component.profileForm.patchValue({
      firstName: 'Johnny',
      lastName: 'Doe',
      phone: '9988776655',
      designation: 'Senior Engineer'
    });

    component.saveProfile();

    const expectedPatch: UpdateProfileRequest = {
      firstName: 'Johnny',
      lastName: 'Doe',
      phone: '9988776655',
      designation: 'Senior Engineer'
    };

    expect(authServiceSpy.updateProfile).toHaveBeenCalledWith(expectedPatch, undefined);
    expect(component.profile()?.firstName).toBe('Johnny');
    expect(component.isEditMode()).toBe(false);
    expect(component.successMessage()).toBe('Profile updated successfully!');
  });

  it('should handle profile load error gracefully and show retry button', () => {
    authServiceSpy.getProfile.mockReturnValue(
      throwError(() => ({ error: { message: 'Database connection error' } }))
    );

    component.loadProfile();
    fixture.detectChanges();

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Database connection error');
  });

  it('should navigate to dashboard when clicking back to dashboard', () => {
    component.navigateToDashboard();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should navigate to tickets when clicking tickets nav item', () => {
    component.navigateToTickets();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/tickets']);
  });

  describe('Change Password Integration', () => {
    it('should open and cancel change password form restoring state', () => {
      expect(component.isChangePasswordOpen()).toBe(false);

      component.openChangePassword();
      expect(component.isChangePasswordOpen()).toBe(true);

      component.changePasswordForm.patchValue({
        currentPassword: 'InitialPassword1!',
        newPassword: 'NewPassword1!',
        confirmNewPassword: 'NewPassword1!'
      });

      component.cancelChangePassword();
      expect(component.isChangePasswordOpen()).toBe(false);
      expect(component.changePasswordForm.get('currentPassword')?.value).toBeFalsy();
      expect(component.changePasswordForm.get('newPassword')?.value).toBeFalsy();
    });

    it('should toggle password visibility flags', () => {
      expect(component.showCurrentPassword()).toBe(false);
      expect(component.showNewPassword()).toBe(false);
      expect(component.showConfirmPassword()).toBe(false);

      component.toggleShowCurrentPassword();
      expect(component.showCurrentPassword()).toBe(true);

      component.toggleShowNewPassword();
      expect(component.showNewPassword()).toBe(true);

      component.toggleShowConfirmPassword();
      expect(component.showConfirmPassword()).toBe(true);
    });

    it('should validate required fields and password complexity', () => {
      component.openChangePassword();
      expect(component.changePasswordForm.invalid).toBe(true);

      // Current password required
      expect(component.currentPasswordControl?.hasError('required')).toBe(true);

      // Weak new password (violates regex pattern)
      component.changePasswordForm.patchValue({
        currentPassword: 'CurrentPassword123!',
        newPassword: 'weak',
        confirmNewPassword: 'weak'
      });
      expect(component.newPasswordControl?.hasError('pattern')).toBe(true);
      expect(component.hasMinimumLength()).toBe(false);

      // Mismatched passwords
      component.changePasswordForm.patchValue({
        currentPassword: 'CurrentPassword123!',
        newPassword: 'ValidPassword123!',
        confirmNewPassword: 'MismatchPassword123!'
      });
      expect(component.changePasswordForm.hasError('passwordMismatch')).toBe(true);
      expect(component.changePasswordForm.invalid).toBe(true);

      // Valid and matching passwords
      component.changePasswordForm.patchValue({
        currentPassword: 'CurrentPassword123!',
        newPassword: 'ValidPassword123!',
        confirmNewPassword: 'ValidPassword123!'
      });
      expect(component.changePasswordForm.valid).toBe(true);
      expect(component.hasMinimumLength()).toBe(true);
      expect(component.hasUppercase()).toBe(true);
      expect(component.hasLowercase()).toBe(true);
      expect(component.hasNumber()).toBe(true);
      expect(component.hasSpecialCharacter()).toBe(true);
    });

    it('should submit change password request to AuthService, close modal, and show success message', () => {
      authServiceSpy.changePassword.mockReturnValue(of({ message: 'Password changed successfully' }));

      component.openChangePassword();
      component.changePasswordForm.patchValue({
        currentPassword: 'CurrentPassword123!',
        newPassword: 'NewSecurePassword123!',
        confirmNewPassword: 'NewSecurePassword123!'
      });

      component.submitChangePassword();

      expect(authServiceSpy.changePassword).toHaveBeenCalledWith({
        currentPassword: 'CurrentPassword123!',
        newPassword: 'NewSecurePassword123!',
        confirmNewPassword: 'NewSecurePassword123!'
      });
      expect(component.isChangingPassword()).toBe(false);
      expect(component.isChangePasswordOpen()).toBe(false);
      expect(component.successMessage()).toBe('Password changed successfully');
    });

    it('should handle change password rejection with error message keeping modal open', () => {
      authServiceSpy.changePassword.mockReturnValue(
        throwError(() => ({ error: { message: 'Current password is incorrect' } }))
      );

      component.openChangePassword();
      component.changePasswordForm.patchValue({
        currentPassword: 'WrongPassword123!',
        newPassword: 'NewSecurePassword123!',
        confirmNewPassword: 'NewSecurePassword123!'
      });

      component.submitChangePassword();

      expect(component.isChangingPassword()).toBe(false);
      expect(component.isChangePasswordOpen()).toBe(true);
      expect(component.passwordErrorMessage()).toBe('Current password is incorrect');
    });

    it('should close modal when clicking backdrop if not submitting', () => {
      component.openChangePassword();
      expect(component.isChangePasswordOpen()).toBe(true);

      const fakeBackdropEvent = {
        target: 'backdrop',
        currentTarget: 'backdrop'
      } as unknown as MouseEvent;

      component.onBackdropClick(fakeBackdropEvent);
      expect(component.isChangePasswordOpen()).toBe(false);
    });

    it('should not close modal when clicking backdrop while submitting', () => {
      component.openChangePassword();
      component.isChangingPassword.set(true);

      const fakeBackdropEvent = {
        target: 'backdrop',
        currentTarget: 'backdrop'
      } as unknown as MouseEvent;

      component.onBackdropClick(fakeBackdropEvent);
      expect(component.isChangePasswordOpen()).toBe(true);
    });

    it('should close modal on Escape key if not submitting', () => {
      component.openChangePassword();
      expect(component.isChangePasswordOpen()).toBe(true);

      component.onEscapeKey();
      expect(component.isChangePasswordOpen()).toBe(false);
    });

    it('should render exactly one Change Password button on the Profile page', () => {
      fixture.detectChanges();
      const heroChangePwdBtn = fixture.nativeElement.querySelector('#btn-hero-change-password');
      expect(heroChangePwdBtn).toBeTruthy();
      expect(heroChangePwdBtn.textContent).toContain('Change Password');

      // Ensure no obsolete Card 3 buttons exist in DOM
      const cardChangePwdBtn = fixture.nativeElement.querySelector('#btn-card-change-password');
      const triggerChangePwdBtn = fixture.nativeElement.querySelector('#btn-trigger-change-password');
      expect(cardChangePwdBtn).toBeNull();
      expect(triggerChangePwdBtn).toBeNull();
    });
  });
});
