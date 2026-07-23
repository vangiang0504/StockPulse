import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin, of, catchError } from 'rxjs';
import { ProductService } from '../products/product.service';
import { WarehouseService } from '../warehouses/warehouse.service';
import { CategoryService } from '../categories/category.service';
import { ProductSummary } from '../products/product.model';
import { AuthService } from '../../core/services/auth.service';

interface CategoryBar { name: string; count: number; pct: number; color: string; }

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

      <!-- Charts (real week-1 data) -->
      <div class="charts">
        <!-- Products by category -->
        <div class="surface-card pad chart-main">
          <div class="panel-head">
            <div>
              <h3 class="panel-title">Products by Category</h3>
              <p class="panel-sub">How the catalog is distributed</p>
            </div>
            <span class="tag">{{ display(productCount) }} total</span>
          </div>

          @if (loading) {
            <p class="muted">Loading…</p>
          } @else if (categoryBreakdownEmpty) {
            <p class="muted">No product data available.</p>
          } @else {
            <div class="bars">
              @for (row of categoryBreakdown; track row.name) {
                <div class="bar-row">
                  <div class="bar-head">
                    <span class="dot" [style.background]="row.color"></span>
                    <span class="bar-name">{{ row.name }}</span>
                    <span class="bar-count num">{{ row.count }}</span>
                  </div>
                  <div class="bar-track">
                    <div class="bar-fill" [style.width.%]="row.pct" [style.background]="row.color"></div>
                  </div>
                </div>
              }
            </div>
          }
        </div>

        <!-- Catalog status donut -->
        <div class="surface-card pad chart-side">
          <h3 class="panel-title">Catalog Status</h3>
          <p class="panel-sub">Active vs inactive products</p>

          @if (loading) {
            <p class="muted">Loading…</p>
          } @else if (statusTotal === 0) {
            <p class="muted">No product data available.</p>
          } @else {
            <div class="donut-wrap">
              <svg viewBox="0 0 42 42" class="donut">
                <circle class="donut-track" cx="21" cy="21" r="15.915" fill="none" stroke-width="4"></circle>
                <circle class="donut-active" cx="21" cy="21" r="15.915" fill="none" stroke-width="4"
                  [attr.stroke-dasharray]="activePct + ' ' + (100 - activePct)" stroke-dashoffset="25"></circle>
              </svg>
              <div class="donut-center">
                <span class="donut-num">{{ statusTotal }}</span>
                <span class="donut-cap">items</span>
              </div>
            </div>
            <div class="legend">
              <div class="legend-row">
                <span class="dot" style="background:#16a34a"></span>
                <span class="legend-name">Active</span>
                <span class="legend-val num">{{ activeCount }}</span>
              </div>
              <div class="legend-row">
                <span class="dot" style="background:#cbd5e1"></span>
                <span class="legend-name">Inactive</span>
                <span class="legend-val num">{{ inactiveCount }}</span>
              </div>
            </div>
          }
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
    .stat-icon { width: 48px; height: 48px; border-radius: 12px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
    .stat-icon mat-icon { font-size: 24px; width: 24px; height: 24px; }
    .stat-title { margin: 0; font-size: 13px; font-weight: 500; color: var(--sp-text-muted); }
    .stat-value { margin: 2px 0 0; font-size: 28px; font-weight: 700; letter-spacing: -0.02em; color: var(--sp-text); }
    .stat-sub { margin: 4px 0 0; font-size: 12px; color: var(--sp-text-faint); }

    /* Charts */
    .charts { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; margin-top: 16px; }
    @media (max-width: 900px) { .charts { grid-template-columns: 1fr; } }
    .panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
    .panel-title { margin: 0; font-size: 14px; font-weight: 600; color: var(--sp-text-soft); }
    .panel-sub { margin: 2px 0 0; font-size: 12px; color: var(--sp-text-faint); }
    .tag { font-size: 11px; font-weight: 600; color: var(--sp-primary-700); background: var(--sp-primary-50); padding: 4px 10px; border-radius: 999px; white-space: nowrap; }
    .muted { color: var(--sp-text-faint); font-size: 13px; padding: 24px 0; text-align: center; }

    .bars { margin-top: 18px; display: flex; flex-direction: column; gap: 14px; }
    .bar-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
    .bar-name { flex: 1; font-size: 13px; color: var(--sp-text-soft); }
    .bar-count { font-size: 13px; font-weight: 600; color: var(--sp-text); }
    .bar-track { height: 8px; background: #f1f5f9; border-radius: 999px; overflow: hidden; }
    .bar-fill { height: 100%; border-radius: 999px; transition: width 0.4s ease; }
    .dot { width: 9px; height: 9px; border-radius: 999px; flex-shrink: 0; display: inline-block; }

    /* Donut */
    .donut-wrap { position: relative; width: 160px; height: 160px; margin: 20px auto 8px; }
    .donut { width: 100%; height: 100%; transform: rotate(0deg); }
    .donut-track { stroke: #eef2f7; }
    .donut-active { stroke: #16a34a; transition: stroke-dasharray 0.5s ease; }
    .donut-center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
    .donut-num { font-size: 26px; font-weight: 700; color: var(--sp-text); line-height: 1; }
    .donut-cap { font-size: 11px; color: var(--sp-text-faint); }
    .legend { display: flex; flex-direction: column; gap: 10px; margin-top: 10px; }
    .legend-row { display: flex; align-items: center; gap: 8px; }
    .legend-name { flex: 1; font-size: 13px; color: var(--sp-text-soft); }
    .legend-val { font-size: 13px; font-weight: 600; color: var(--sp-text); }

    .section-title { margin: 28px 0 14px; font-size: 14px; font-weight: 600; color: var(--sp-text-soft); }

    .modules { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; }
    .module { display: flex; align-items: center; gap: 14px; padding: 18px;
      background: var(--sp-surface); border: 1px solid var(--sp-border); border-radius: var(--sp-radius);
      box-shadow: var(--sp-shadow-sm); text-decoration: none; transition: box-shadow 0.15s, transform 0.15s, border-color 0.15s; }
    .module:hover { box-shadow: var(--sp-shadow-md); transform: translateY(-1px); border-color: #d7deea; }
    .module-icon { width: 42px; height: 42px; border-radius: 11px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
    .module-icon mat-icon { font-size: 21px; width: 21px; height: 21px; }
    .module-body { flex: 1; min-width: 0; }
    .module-title { margin: 0; font-size: 14px; font-weight: 600; color: var(--sp-text); }
    .module-desc { margin: 2px 0 0; font-size: 12px; color: var(--sp-text-faint); }
    .module-arrow { color: var(--sp-text-faint); flex-shrink: 0; }

    .roadmap { display: flex; flex-wrap: wrap; gap: 10px; }
    .roadmap-item { display: flex; align-items: center; gap: 8px; padding: 8px 14px;
      background: #fff; border: 1px dashed var(--sp-border); border-radius: 999px;
      color: var(--sp-text-muted); font-size: 13px; font-weight: 500; }
    .roadmap-item mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--sp-text-faint); }

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

  loading = true;
  categoryBreakdown: CategoryBar[] = [];
  activeCount = 0;
  inactiveCount = 0;

  readonly username = this.authService.getUsername();
  readonly upcoming = [
    { icon: 'layers', label: 'Stock Levels' },
    { icon: 'swap_horiz', label: 'Movements' },
    { icon: 'notifications_active', label: 'Alerts & Reorder' },
    { icon: 'monitoring', label: 'Reporting' }
  ];

  /** Palette cycled across category bars. */
  private readonly palette = ['#2563eb', '#7c3aed', '#d97706', '#16a34a', '#dc2626', '#0891b2', '#db2777'];

  constructor(
    private productService: ProductService,
    private warehouseService: WarehouseService,
    private categoryService: CategoryService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Categories give us id → name for the bars; products give the distribution.
    forkJoin({
      categories: this.categoryService.getCategories(0, 200).pipe(catchError(() => of(null))),
      products: this.productService.getProducts(0, 500).pipe(catchError(() => of(null)))
    }).subscribe(({ categories, products }) => {
      const nameById = new Map<number, string>();
      if (categories?.success && categories.data) {
        this.categoryCount = categories.data.totalElements;
        for (const c of categories.data.content) nameById.set(c.id, c.name);
      }
      if (products?.success && products.data) {
        this.productCount = products.data.totalElements;
        this.buildBreakdown(products.data.content, nameById);
      }
      this.loading = false;
    });

    this.warehouseService.getWarehouses(0, 1).pipe(catchError(() => of(null))).subscribe(res => {
      if (res?.success && res.data) this.warehouseCount = res.data.totalElements;
    });
  }

  get statusTotal(): number { return this.activeCount + this.inactiveCount; }
  get activePct(): number { return this.statusTotal ? (this.activeCount / this.statusTotal) * 100 : 0; }
  get categoryBreakdownEmpty(): boolean { return this.categoryBreakdown.length === 0; }

  private buildBreakdown(products: ProductSummary[], nameById: Map<number, string>): void {
    let active = 0;
    let inactive = 0;
    const counts = new Map<number, number>();
    for (const p of products) {
      p.active ? active++ : inactive++;
      counts.set(p.categoryId, (counts.get(p.categoryId) ?? 0) + 1);
    }
    this.activeCount = active;
    this.inactiveCount = inactive;

    let rows = [...counts.entries()]
      .map(([id, count]) => ({ name: nameById.get(id) ?? `Category #${id}`, count }))
      .sort((a, b) => b.count - a.count);

    // Collapse the long tail so the chart stays readable.
    if (rows.length > 6) {
      const top = rows.slice(0, 6);
      const other = rows.slice(6).reduce((sum, r) => sum + r.count, 0);
      top.push({ name: 'Other', count: other });
      rows = top;
    }

    const max = Math.max(...rows.map(r => r.count), 1);
    this.categoryBreakdown = rows.map((r, i) => ({
      ...r,
      pct: Math.round((r.count / max) * 100),
      color: this.palette[i % this.palette.length]
    }));
  }

  display(value: number | null): string {
    return value === null ? '—' : value.toLocaleString();
  }
}
