import { Component, output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardStateService } from '../../services/dashboard-state.service';

@Component({
  selector: 'app-resolve-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './resolve-modal.html',
  styleUrls: ['./resolve-modal.css']
})
export class ResolveModalComponent {
  readonly state = inject(DashboardStateService);

  actionCompleted = output<void>();

  confirmResolve(): void {
    this.state.confirmResolveTicket(() => {
      this.actionCompleted.emit();
    });
  }
}
