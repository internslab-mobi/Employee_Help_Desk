import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { UserRole } from '../../core/models/auth.models';
import { EmployeeDashboardComponent } from './pages/employee-dashboard/employee-dashboard';
import { AgentDashboard } from './pages/agent-dashboard/agent-dashboard';
import { ManagerDashboardComponent } from './pages/manager-dashboard/manager-dashboard';
import { AdminDashboardComponent } from './pages/admin-dashboard/admin-dashboard';

export type AnalyticsDashboardView =
  | 'OVERALL_OVERVIEW'
  | 'TICKET_ANALYTICS';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    EmployeeDashboardComponent,
    AgentDashboard,
    ManagerDashboardComponent,
    AdminDashboardComponent
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent {
  private authService = inject(AuthService);
  readonly userRole = computed<UserRole>(() => this.authService.getRole() || 'EMPLOYEE');
}
