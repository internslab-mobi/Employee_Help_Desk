
import { Component } from '@angular/core';

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [],
  templateUrl: './agent-dashboard.html',
  styleUrl: './agent-dashboard.css'
})
export class AgentDashboard {

  // Temporary values for UI development.
  // Replace these with API data later.
  totalAssigned = 18;
  inProgress = 7;
  slaAtRisk = 3;
  resolvedToday = 5;

}