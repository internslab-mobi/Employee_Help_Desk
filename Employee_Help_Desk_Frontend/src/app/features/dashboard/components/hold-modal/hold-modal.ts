import { Component, output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardStateService } from '../../services/dashboard-state.service';

@Component({
  selector: 'app-hold-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './hold-modal.html',
  styleUrls: ['./hold-modal.css']
})
export class HoldModalComponent {
  readonly state = inject(DashboardStateService);

  actionCompleted = output<void>();

  confirmHold(): void {
    this.state.confirmHoldTicket(() => {
      this.actionCompleted.emit();
    });
  }
}
