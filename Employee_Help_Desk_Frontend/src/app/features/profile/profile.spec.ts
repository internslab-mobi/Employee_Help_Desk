import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ProfileComponent } from './profile';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeProfile, UpdateProfileRequest } from '../../core/models/auth.models';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
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
    authServiceSpy = {
      getProfile: vi.fn().mockReturnValue(of(mockProfile)),
      updateProfile: vi.fn().mockReturnValue(of(mockProfile)),
      logout: vi.fn()
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
});
