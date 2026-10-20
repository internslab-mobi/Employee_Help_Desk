import { Component, input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardStateService } from '../../services/dashboard-state.service';

@Component({
  selector: 'app-report-summary-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './report-summary-modal.html',
  styleUrls: ['./report-summary-modal.css']
})
export class ReportSummaryModalComponent {
  readonly state = inject(DashboardStateService);

  dateRange = input<{ from?: string; to?: string }>({});
  departmentId = input<number | null>(null);

  exportExcel(): void {
    this.state.exportSummaryToExcel(this.dateRange(), this.departmentId());
  }
}
