import { Component, Input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  template: `
    @if (loading) {
      <div class="spinner-overlay">
        <mat-spinner [diameter]="diameter"></mat-spinner>
      </div>
    }
  `,
  styles: [`
    .spinner-overlay {
      position: fixed; top: 0; left: 0; width: 100%; height: 100%;
      display: flex; justify-content: center; align-items: center;
      background: rgba(255, 255, 255, 0.7); z-index: 1000;
    }
  `]
})
export class LoadingSpinnerComponent {
  @Input() loading = false;
  @Input() diameter = 50;
}
