import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="auth-bg">
      <div class="auth-card">
        <div class="auth-brand">
          <span class="brand-mark">
            <svg width="20" height="20" viewBox="0 0 16 16" fill="none">
              <rect x="1" y="1" width="6" height="6" rx="1.5" fill="white" fill-opacity=".9" />
              <rect x="9" y="1" width="6" height="6" rx="1.5" fill="white" fill-opacity=".5" />
              <rect x="1" y="9" width="6" height="6" rx="1.5" fill="white" fill-opacity=".5" />
              <rect x="9" y="9" width="6" height="6" rx="1.5" fill="white" fill-opacity=".9" />
            </svg>
          </span>
          <span class="brand-name">Stock<span class="brand-accent">Pulse</span></span>
        </div>
        <p class="auth-tagline">Warehouse inventory management</p>
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styles: [`
    .auth-bg {
      display: flex; justify-content: center; align-items: center; min-height: 100vh; padding: 24px;
      background: radial-gradient(1200px 600px at 30% -10%, #1d3166 0%, transparent 60%),
                  linear-gradient(135deg, #0d1b3e 0%, #162650 100%);
    }
    .auth-card {
      width: 400px; max-width: 100%; background: #fff; padding: 36px 32px;
      border-radius: 16px; box-shadow: 0 20px 50px rgba(5, 14, 37, 0.35);
    }
    .auth-brand { display: flex; align-items: center; justify-content: center; gap: 10px; }
    .brand-mark {
      width: 38px; height: 38px; border-radius: 11px; display: flex; align-items: center; justify-content: center;
      background: linear-gradient(135deg, #3b82f6, #1d4ed8);
    }
    .brand-name { font-size: 22px; font-weight: 700; letter-spacing: -0.02em; color: #0d1b3e; }
    .brand-accent { color: #2563eb; }
    .auth-tagline { text-align: center; color: var(--sp-text-faint); font-size: 13px; margin: 8px 0 28px; }
  `]
})
export class AuthLayoutComponent {}
