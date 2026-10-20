import { Component, input, output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardStateService } from '../../services/dashboard-state.service';

@Component({
  selector: 'app-dashboard-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-sidebar.html',
  styleUrls: ['./dashboard-sidebar.css']
})
export class DashboardSidebarComponent {
  readonly state = inject(DashboardStateService);

  activeNav = input<string>('dashboard');
  navChange = output<string>();

  onNavClick(nav: string): void {
    this.navChange.emit(nav);
  }
}
