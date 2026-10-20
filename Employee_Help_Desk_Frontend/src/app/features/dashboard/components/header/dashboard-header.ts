import { Component, input, output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardStateService } from '../../services/dashboard-state.service';
import { AnalyticsDashboardView } from '../../dashboard';

@Component({
  selector: 'app-dashboard-header',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard-header.html',
  styleUrls: ['./dashboard-header.css']
})
export class DashboardHeaderComponent {
  readonly state = inject(DashboardStateService);

  activeNav = input<string>('dashboard');
  pageTitle = input<string>('');
  pageSubtitle = input<string>('');
  selectedAnalyticsView = input<AnalyticsDashboardView>('OVERALL_OVERVIEW');

  analyticsViewChange = output<AnalyticsDashboardView>();
  refresh = output<void>();

  onAnalyticsViewChange(view: AnalyticsDashboardView): void {
    this.analyticsViewChange.emit(view);
  }

  onRefreshClick(): void {
    this.refresh.emit();
  }
}
