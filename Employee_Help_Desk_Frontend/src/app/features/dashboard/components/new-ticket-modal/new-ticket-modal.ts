import { Component, output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardStateService } from '../../services/dashboard-state.service';

@Component({
  selector: 'app-new-ticket-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './new-ticket-modal.html',
  styleUrls: ['./new-ticket-modal.css']
})
export class NewTicketModalComponent {
  readonly state = inject(DashboardStateService);

  ticketCreated = output<void>();

  onSubmit(): void {
    this.state.submitNewTicket(() => {
      this.ticketCreated.emit();
    });
  }
}
