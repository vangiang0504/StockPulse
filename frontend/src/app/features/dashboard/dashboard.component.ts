import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { ProductService } from '../products/product.service';
import { WarehouseService } from '../warehouses/warehouse.service';
import { CategoryService } from '../categories/category.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MatIconModule],
  template: `
    <div class="page">
      <div class="hero">
        <h2>Welcome back{{ username ? ', ' + username : '' }} 👋</h2>
        <p>Here's the current state of your StockPulse catalog.</p>
      </div>

      <!-- Stat cards -->
      <div class="stats">
        <div class="stat">
          <div class="stat-icon blue"><mat-icon>inventory_2</mat-icon></div>
          <div class="stat-body">
            <p class="stat-title">Total Products</p>
            <p class="stat-value">{{ display(productCount) }}</p>
            <p class="stat-sub">Registered in the catalog</p>
          </div>
        </div>
        <div class="stat">
          <div class="stat-icon violet"><mat-icon>warehouse</mat-icon></div>
          <div class="stat-body">
            <p class="stat-title">Warehouses</p>
            <p class="stat-value">{{ display(warehouseCount) }}</p>
            <p class="stat-sub">Active storage locations</p>
          </div>
        </div>
        <div class="stat">
          <div class="stat-icon amber"><mat-icon>category</mat-icon></div>
          <div class="stat-body">
            <p class="stat-title">Categories</p>
            <p class="stat-value">{{ display(categoryCount) }}</p>
            <p class="stat-sub">Product classification tree</p>
          </div>
        </div>
      </div>

      <!-- Quick access -->
      <h3 class="section-title">Quick access</h3>
      <div class="modules">
        <a class="module" routerLink="/products">
          <div class="module-icon blue"><mat-icon>inventory_2</mat-icon></div>
          <div class="module-body">
            <p class="module-title">Products</p>
            <p class="module-desc">Browse and manage the product catalog</p>
          </div>
          <mat-icon class="module-arrow">chevron_right</mat-icon>
        </a>
        <a class="module" routerLink="/warehouses">
          <div class="module-icon violet"><mat-icon>warehouse</mat-icon></div>
          <div class="module-body">
            <p class="module-title">Warehouses</p>
            <p class="module-desc">View storage locations and status</p>
          </div>
          <mat-icon class="module-arrow">chevron_right</mat-icon>
        </a>
        <a class="module" routerLink="/users">
          <div class="module-icon slate"><mat-icon>people</mat-icon></div>
          <div class="module-body">
            <p class="module-title">Users</p>
            <p class="module-desc">Manage system accounts and roles</p>
          </div>
          <mat-icon class="module-arrow">chevron_right</mat-icon>
        </a>
      </div>

      <!-- Roadmap -->
      <h3 class="section-title">Coming soon</h3>
      <div class="roadmap">
        @for (item of upcoming; track item.label) {
          <div class="roadmap-item">
            <mat-icon>{{ item.icon }}</mat-icon>
            <span>{{ item.label }}</span>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .hero { margin-bottom: 20px; }
    .hero h2 { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.01em; color: var(--sp-text); }
    .hero p { margin: 4px 0 0; color: var(--sp-text-faint); font-size: 14px; }

    .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
    .stat {
      display: flex; align-items: flex-start; gap: 16px; padding: 20px;
      background: var(--sp-surface); border: 1px solid var(--sp-border);
      border-radius: var(--sp-radius); box-shadow: var(--sp-shadow-sm); transition: box-shadow 0.15s;
    }
    .stat:hover { box-shadow: var(--sp-shadow-md); }
    .stat-icon {
      width: 48px; height: 48px; border-radius: 12px; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
    }
    .stat-icon mat-icon { font-size: 24px; width: 24px; height: 24px; }
    .stat-title { margin: 0; font-size: 13px; font-weight: 500; color: var(--sp-text-muted); }
    .stat-value { margin: 2px 0 0; font-size: 28px; font-weight: 700; letter-spacing: -0.02em; color: var(--sp-text); }
    .stat-sub { margin: 4px 0 0; font-size: 12px; color: var(--sp-text-faint); }

    .section-title { margin: 28px 0 14px; font-size: 14px; font-weight: 600; color: var(--sp-text-soft); }

    .modules { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; }
    .module {
      display: flex; align-items: center; gap: 14px; padding: 18px;
      background: var(--sp-surface); border: 1px solid var(--sp-border);
      border-radius: var(--sp-radius); box-shadow: var(--sp-shadow-sm);
      text-decoration: none; transition: box-shadow 0.15s, transform 0.15s, border-color 0.15s;
    }
    .module:hover { box-shadow: var(--sp-shadow-md); transform: translateY(-1px); border-color: #d7deea; }
    .module-icon {
      width: 42px; height: 42px; border-radius: 11px; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
    }
    .module-icon mat-icon { font-size: 21px; width: 21px; height: 21px; }
    .module-body { flex: 1; min-width: 0; }
    .module-title { margin: 0; font-size: 14px; font-weight: 600; color: var(--sp-text); }
    .module-desc { margin: 2px 0 0; font-size: 12px; color: var(--sp-text-faint); }
    .module-arrow { color: var(--sp-text-faint); flex-shrink: 0; }

    .roadmap { display: flex; flex-wrap: wrap; gap: 10px; }
    .roadmap-item {
      display: flex; align-items: center; gap: 8px; padding: 8px 14px;
      background: #fff; border: 1px dashed var(--sp-border); border-radius: 999px;
      color: var(--sp-text-muted); font-size: 13px; font-weight: 500;
    }
    .roadmap-item mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--sp-text-faint); }

    /* Icon tile palettes */
    .blue { background: #eff6ff; color: #2563eb; }
    .violet { background: #f5f3ff; color: #7c3aed; }
    .amber { background: #fffbeb; color: #d97706; }
    .slate { background: #f1f5f9; color: #475569; }
  `]
})
export class DashboardComponent implements OnInit {
  productCount: number | null = null;
  warehouseCount: number | null = null;
  categoryCount: number | null = null;

  readonly username = this.authService.getUsername();
  readonly upcoming = [
    { icon: 'layers', label: 'Stock Levels' },
    { icon: 'swap_horiz', label: 'Movements' },
    { icon: 'notifications_active', label: 'Alerts & Reorder' },
    { icon: 'monitoring', label: 'Reporting' }
  ];

  constructor(
    private productService: ProductService,
    private warehouseService: WarehouseService,
    private categoryService: CategoryService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // A page size of 1 is enough: we only read totalElements for the headline counts.
    this.productService.getProducts(0, 1).subscribe({
      next: res => { if (res.success && res.data) this.productCount = res.data.totalElements; }
    });
    this.warehouseService.getWarehouses(0, 1).subscribe({
      next: res => { if (res.success && res.data) this.warehouseCount = res.data.totalElements; }
    });
    this.categoryService.getCategories(0, 1).subscribe({
      next: res => { if (res.success && res.data) this.categoryCount = res.data.totalElements; }
    });
  }

  display(value: number | null): string {
    return value === null ? '—' : value.toLocaleString();
  }
}
