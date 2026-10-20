import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardStateService } from '../../services/dashboard-state.service';

@Component({
  selector: 'app-notification-drawer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-drawer.html',
  styleUrls: ['./notification-drawer.css']
})
export class NotificationDrawerComponent {
  readonly state = inject(DashboardStateService);
}
