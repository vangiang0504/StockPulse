import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule, MatIconModule],
  template: `
    <h2>Dashboard</h2>
    <div class="cards">
      <mat-card class="dashboard-card">
        <mat-card-header>
          <mat-icon mat-card-avatar>people</mat-icon>
          <mat-card-title>Users</mat-card-title>
          <mat-card-subtitle>Manage system users</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p>Use the Users page to view and manage user accounts. This is the reference CRUD implementation.</p>
        </mat-card-content>
      </mat-card>

      <mat-card class="dashboard-card">
        <mat-card-header>
          <mat-icon mat-card-avatar>code</mat-icon>
          <mat-card-title>Starter Codebase</mat-card-title>
          <mat-card-subtitle>Extend with your domain</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p>Follow the User CRUD pattern to add your domain entities, services, and Angular pages.</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; margin-top: 16px; }
    .dashboard-card { cursor: default; }
  `]
})
export class DashboardComponent {}
