import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { EmployeeProfile, UpdateProfileRequest, UserRole } from '../../core/models/auth.models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class ProfileComponent implements OnInit {
  readonly profile = signal<EmployeeProfile | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly isSaving = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly isEditMode = signal<boolean>(false);
  readonly selectedImage = signal<File | null>(null);
  readonly imagePreviewUrl = signal<string | null>(null);

  profileForm!: FormGroup;

  readonly displayUserName = computed(() => {
    const p = this.profile();
    if (!p) return 'User';
    return `${p.firstName} ${p.lastName || ''}`.trim() || p.email;
  });

  readonly displayUserRole = computed(() => {
    const role = this.profile()?.role;
    if (role === 'EMPLOYEE') return 'Employee';
    if (role === 'AGENT') return 'Support Agent';
    if (role === 'MANAGER') return 'Department Manager';
    if (role === 'ADMIN') return 'Administrator';
    return 'Employee';
  });

  // Role and auth signals
  readonly userRole = computed<UserRole>(() => (typeof this.authService?.getRole === 'function' ? (this.authService.getRole() ?? 'EMPLOYEE') : 'EMPLOYEE'));

  constructor(
    private fb: FormBuilder,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadProfile();
  }

  private initForm(): void {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(50)]],
      lastName: ['', [Validators.maxLength(50)]],
      phone: ['', [Validators.maxLength(20)]],
      designation: ['', [Validators.maxLength(100)]]
    });
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.getProfile().subscribe({
      next: (profileData: EmployeeProfile) => {
        this.profile.set(profileData);
        this.populateForm(profileData);
        this.isLoading.set(false);
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err?.error?.message || 'Failed to load profile details from server. Please try again.'
        );
      }
    });
  }

  private populateForm(data: EmployeeProfile): void {
    this.profileForm.patchValue({
      firstName: data.firstName || '',
      lastName: data.lastName || '',
      phone: data.phone || '',
      designation: data.designation || ''
    });
    this.profileForm.markAsPristine();
  }

  enterEditMode(): void {
    this.isEditMode.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);
    if (this.profile()) {
      this.populateForm(this.profile()!);
    }
  }

  cancelEdit(): void {
    this.isEditMode.set(false);
    this.errorMessage.set(null);
    this.selectedImage.set(null);
    this.imagePreviewUrl.set(null);
    if (this.profile()) {
      this.populateForm(this.profile()!);
    }
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (!['image/jpeg', 'image/png'].includes(file.type)) {
        this.errorMessage.set('Only JPG and PNG images are supported.');
        return;
      }
      this.selectedImage.set(file);
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreviewUrl.set(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const formValues = this.profileForm.value;
    const patchPayload: UpdateProfileRequest = {
      firstName: formValues.firstName.trim(),
      lastName: formValues.lastName ? formValues.lastName.trim() : undefined,
      phone: formValues.phone ? formValues.phone.trim() : undefined,
      designation: formValues.designation ? formValues.designation.trim() : undefined
    };

    this.authService.updateProfile(patchPayload, this.selectedImage() || undefined).subscribe({
      next: (updatedProfile: EmployeeProfile) => {
        this.profile.set(updatedProfile);
        this.populateForm(updatedProfile);
        this.isSaving.set(false);
        this.isEditMode.set(false);
        this.selectedImage.set(null);
        this.imagePreviewUrl.set(null);
        this.successMessage.set('Profile updated successfully!');
      },
      error: (err: any) => {
        this.isSaving.set(false);
        this.errorMessage.set(
          err?.error?.message || 'Failed to update profile. Please verify your inputs and try again.'
        );
      }
    });
  }

  navigateToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  navigateToTickets(): void {
    this.router.navigate(['/tickets']);
  }

  navigateToAssigned(): void {
    this.router.navigate(['/dashboard'], { queryParams: { tab: 'assigned' } });
  }

  navigateToAssign(): void {
    this.router.navigate(['/dashboard'], { queryParams: { tab: 'assign' } });
  }

  logout(): void {
    this.authService.logout();
  }

  getInitials(name?: string): string {
    if (!name) return 'U';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return parts[0].slice(0, 2).toUpperCase();
  }
}
