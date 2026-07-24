import { Component } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatButtonModule],
  template: `
    <div class="shell" [class.collapsed]="collapsed">
      <!-- ── Sidebar ── -->
      <aside class="sidebar">
        <div class="brand">
          <span class="brand-mark">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <rect x="1" y="1" width="6" height="6" rx="1.5" fill="white" fill-opacity=".9" />
              <rect x="9" y="1" width="6" height="6" rx="1.5" fill="white" fill-opacity=".5" />
              <rect x="1" y="9" width="6" height="6" rx="1.5" fill="white" fill-opacity=".5" />
              <rect x="9" y="9" width="6" height="6" rx="1.5" fill="white" fill-opacity=".9" />
            </svg>
          </span>
          @if (!collapsed) {
            <span class="brand-name">Stock<span class="brand-accent">Pulse</span></span>
          }
        </div>

        <nav class="nav">
          @if (!collapsed) { <p class="nav-label">Main Menu</p> }

          <a routerLink="/dashboard" routerLinkActive="active" class="nav-item" [title]="collapsed ? 'Dashboard' : ''">
            <mat-icon>dashboard</mat-icon>
            @if (!collapsed) { <span>Dashboard</span> }
          </a>
          <a routerLink="/products" routerLinkActive="active" class="nav-item" [title]="collapsed ? 'Products' : ''">
            <mat-icon>inventory_2</mat-icon>
            @if (!collapsed) { <span>Products</span> }
          </a>
          @if (canViewWarehouses()) {
            <a routerLink="/warehouses" routerLinkActive="active" class="nav-item" [title]="collapsed ? 'Warehouses' : ''">
              <mat-icon>warehouse</mat-icon>
              @if (!collapsed) { <span>Warehouses</span> }
            </a>
          }
          <a routerLink="/users" routerLinkActive="active" class="nav-item" [title]="collapsed ? 'Users' : ''">
            <mat-icon>people</mat-icon>
            @if (!collapsed) { <span>Users</span> }
          </a>
        </nav>
      </aside>

      <!-- ── Main column ── -->
      <div class="main">
        <header class="topbar">
          <button class="icon-btn" (click)="collapsed = !collapsed" aria-label="Toggle navigation">
            <mat-icon>menu</mat-icon>
          </button>

          <div class="title-block">
            <h1>{{ pageTitle }}</h1>
            <p>{{ today }}</p>
          </div>

          <span class="spacer"></span>

          <div class="user">
            <div class="avatar">{{ initials }}</div>
            <div class="user-meta">
              <p class="user-name">{{ authService.getUsername() }}</p>
              <p class="user-role">{{ roleLabel }}</p>
            </div>
          </div>
          <button class="icon-btn" (click)="authService.logout()" aria-label="Sign out" title="Sign out">
            <mat-icon>logout</mat-icon>
          </button>
        </header>

        <main class="content">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .shell { display: flex; height: 100vh; overflow: hidden; background: var(--sp-bg); }

    /* Sidebar */
    .sidebar {
      width: 240px; flex-shrink: 0; display: flex; flex-direction: column;
      background: var(--sp-navy-900); transition: width 0.25s ease; overflow: hidden;
    }
    .shell.collapsed .sidebar { width: 72px; }

    .brand {
      display: flex; align-items: center; gap: 12px; height: 64px; flex-shrink: 0;
      padding: 0 20px; border-bottom: 1px solid var(--sp-navy-700);
    }
    .brand-mark {
      width: 32px; height: 32px; flex-shrink: 0; border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      background: linear-gradient(135deg, #3b82f6, #1d4ed8);
    }
    .brand-name { color: #fff; font-weight: 700; font-size: 15px; letter-spacing: -0.01em; white-space: nowrap; }
    .brand-accent { color: var(--sp-navy-accent); }

    .nav { flex: 1; padding: 12px 0; overflow-y: auto; overflow-x: hidden; }
    .nav-label {
      margin: 0 0 8px; padding: 0 20px; font-size: 10px; font-weight: 700;
      text-transform: uppercase; letter-spacing: 0.1em; color: var(--sp-navy-label);
    }
    .nav-item {
      position: relative; display: flex; align-items: center; gap: 12px;
      padding: 11px 20px; color: var(--sp-navy-muted); text-decoration: none;
      font-size: 14px; font-weight: 500; white-space: nowrap;
      border-left: 3px solid transparent; transition: background 0.15s, color 0.15s;
    }
    .nav-item mat-icon { font-size: 20px; width: 20px; height: 20px; flex-shrink: 0; }
    .nav-item:hover { color: #fff; background: rgba(255, 255, 255, 0.05); }
    .nav-item.active {
      color: #fff; background: rgba(59, 130, 246, 0.18); border-left-color: var(--sp-navy-accent);
    }
    .shell.collapsed .nav-item { justify-content: center; padding: 11px 0; }

    /* Main column */
    .main { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-width: 0; }

    .topbar {
      height: 64px; flex-shrink: 0; display: flex; align-items: center; gap: 16px;
      padding: 0 20px; background: var(--sp-surface); border-bottom: 1px solid var(--sp-border); z-index: 5;
    }
    .title-block h1 { margin: 0; font-size: 15px; font-weight: 700; color: var(--sp-text); line-height: 1.2; }
    .title-block p { margin: 0; font-size: 12px; color: var(--sp-text-faint); }

    .icon-btn {
      display: inline-flex; align-items: center; justify-content: center;
      width: 38px; height: 38px; border: none; background: transparent; cursor: pointer;
      border-radius: var(--sp-radius-sm); color: var(--sp-text-faint); transition: background 0.15s, color 0.15s;
    }
    .icon-btn:hover { background: #f3f4f6; color: var(--sp-text-soft); }

    .user { display: flex; align-items: center; gap: 10px; }
    .avatar {
      width: 34px; height: 34px; border-radius: 999px; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
      color: #fff; font-size: 13px; font-weight: 700;
      background: linear-gradient(135deg, #3b82f6, #1d4ed8);
    }
    .user-meta { line-height: 1.25; }
    .user-name { margin: 0; font-size: 13px; font-weight: 600; color: var(--sp-text-soft); }
    .user-role { margin: 0; font-size: 11px; color: var(--sp-text-faint); }

    .content { flex: 1; overflow-y: auto; }

    @media (max-width: 640px) {
      .user-meta { display: none; }
    }
  `]
})
export class MainLayoutComponent {
  collapsed = false;

  constructor(public authService: AuthService, private router: Router) {}

  canViewWarehouses(): boolean {
    const role = this.authService.getRole()?.replace(/^ROLE_/, '');
    return role === 'STAFF' || role === 'MANAGER' || role === 'ADMIN';
  }

  get pageTitle(): string {
    const url = this.router.url.split('?')[0];
    if (url.startsWith('/products/create')) return 'New Product';
    if (url.startsWith('/products') && url.includes('/edit')) return 'Edit Product';
    if (url.startsWith('/products')) return 'Product Management';
    if (url.startsWith('/warehouses')) return 'Warehouse Management';
    if (url.startsWith('/users/new')) return 'New User';
    if (url.startsWith('/users') && url.includes('/edit')) return 'Edit User';
    if (url.startsWith('/users')) return 'User Management';
    return 'Stock Dashboard';
  }

  get today(): string {
    return new Date().toLocaleDateString('en-US', {
      weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
    });
  }

  get roleLabel(): string {
    const role = this.authService.getRole()?.replace(/^ROLE_/, '') ?? '';
    if (!role) return 'StockPulse';
    return role.charAt(0) + role.slice(1).toLowerCase();
  }

  get initials(): string {
    const name = this.authService.getUsername() ?? '';
    const parts = name.trim().split(/[\s._-]+/).filter(Boolean);
    if (parts.length === 0) return 'SP';
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
}
