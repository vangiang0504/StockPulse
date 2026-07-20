import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="auth-container">
      <div class="auth-card">
        <h2 class="auth-title">Training Starter</h2>
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styles: [`
    .auth-container {
      display: flex; justify-content: center; align-items: center;
      min-height: 100vh; background-color: #f5f5f5;
    }
    .auth-card {
      background: white; padding: 40px; border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1); width: 400px; max-width: 90%;
    }
    .auth-title { text-align: center; margin-bottom: 24px; color: #3f51b5; }
  `]
})
export class AuthLayoutComponent {}
