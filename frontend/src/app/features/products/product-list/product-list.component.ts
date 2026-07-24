import { Component, OnInit, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProductService } from '../product.service';
import { ProductSummary } from '../product.model';
import { CategoryService } from '../../categories/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [RouterLink, MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressSpinnerModule],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h2>Products</h2>
          <p class="page-subtitle">{{ totalElements }} products registered</p>
        </div>
        @if (canManage) {
          <a mat-raised-button color="primary" routerLink="/products/create">
            <mat-icon>add</mat-icon> New Product
          </a>
        }
      </div>

      @if (loading) {
        <div class="list-state">
          <mat-spinner diameter="40"></mat-spinner>
        </div>
      } @else if (products.length === 0) {
        <div class="list-state empty">
          <mat-icon>inventory_2</mat-icon>
          <p>No products found.</p>
        </div>
      } @else {
        <div class="surface-card">
          <table mat-table [dataSource]="products" class="full-width">
            <ng-container matColumnDef="sku">
              <th mat-header-cell *matHeaderCellDef>SKU</th>
              <td mat-cell *matCellDef="let product"><span class="sku-chip">{{ product.sku }}</span></td>
            </ng-container>

            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Name</th>
              <td mat-cell *matCellDef="let product"><span class="cell-strong">{{ product.name }}</span></td>
            </ng-container>

            <ng-container matColumnDef="categoryId">
              <th mat-header-cell *matHeaderCellDef>Category</th>
              <td mat-cell *matCellDef="let product"><span class="category-pill">{{ categoryName(product.categoryId) }}</span></td>
            </ng-container>

            <ng-container matColumnDef="unit">
              <th mat-header-cell *matHeaderCellDef>Unit</th>
              <td mat-cell *matCellDef="let product"><span class="num">{{ product.unit }}</span></td>
            </ng-container>

            <ng-container matColumnDef="minStock">
              <th mat-header-cell *matHeaderCellDef>Min Stock</th>
              <td mat-cell *matCellDef="let product"><span class="num">{{ product.minStock }}</span></td>
            </ng-container>

            <ng-container matColumnDef="reorderPoint">
              <th mat-header-cell *matHeaderCellDef>Reorder Point</th>
              <td mat-cell *matCellDef="let product"><span class="num">{{ product.reorderPoint }}</span></td>
            </ng-container>

            <ng-container matColumnDef="active">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let product">
                <mat-chip [highlighted]="product.active">{{ product.active ? 'Active' : 'Inactive' }}</mat-chip>
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Actions</th>
              <td mat-cell *matCellDef="let product">
                @if (canManage) {
                  <a mat-icon-button [routerLink]="['/products', product.id, 'edit']"><mat-icon>edit</mat-icon></a>
                }
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          <mat-paginator [length]="totalElements" [pageSize]="pageSize" [pageSizeOptions]="[10, 20, 50]"
                         (page)="onPageChange($event)"></mat-paginator>
        </div>
      }
    </div>
  `,
  styles: [`
    .list-state { display: flex; flex-direction: column; align-items: center; justify-content: center;
      min-height: 200px; color: var(--sp-text-faint); gap: 8px;
      background: var(--sp-surface); border: 1px solid var(--sp-border); border-radius: var(--sp-radius); }
    .list-state.empty mat-icon { font-size: 48px; width: 48px; height: 48px; }
    .cell-strong { font-weight: 500; color: var(--sp-text); }
  `]
})
export class ProductListComponent implements OnInit {
  products: ProductSummary[] = [];
  displayedColumns = ['sku', 'name', 'categoryId', 'unit', 'minStock', 'reorderPoint', 'active', 'actions'];
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  loading = false;

  /** categoryId -> category name, so the table can show a label instead of a raw id. */
  private categoryNames = new Map<number, string>();

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private productService: ProductService,
    private categoryService: CategoryService,
    private notification: NotificationService,
    private authService: AuthService
  ) {}

  /**
   * Creating and updating products is restricted to MANAGER and ADMIN on the API,
   * so STAFF should not be offered actions that would come back as 403.
   */
  get canManage(): boolean {
    const role = this.authService.getRole();
    return role === 'MANAGER' || role === 'ADMIN';
  }

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.categoryService.getCategories().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.categoryNames = new Map(res.data.content.map(c => [c.id, c.name]));
        }
      },
      error: () => this.notification.error('Failed to load categories')
    });
  }

  loadProducts(): void {
    this.loading = true;
    this.productService.getProducts(this.currentPage, this.pageSize).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.products = res.data.content;
          this.totalElements = res.data.totalElements;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load products');
      }
    });
  }

  categoryName(id: number): string {
    return this.categoryNames.get(id) ?? '—';
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }
}
