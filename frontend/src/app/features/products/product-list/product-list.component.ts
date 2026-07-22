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
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [RouterLink, MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressSpinnerModule],
  template: `
    <div style="display: flex; justify-content: space-between; align-items: center;">
      <h2>Products</h2>
      <a mat-raised-button color="primary" routerLink="/products/create">
        <mat-icon>add</mat-icon> New Product
      </a>
    </div>

    @if (loading) {
      <div style="display: flex; justify-content: center; padding: 48px;">
        <mat-spinner diameter="40"></mat-spinner>
      </div>
    } @else if (products.length === 0) {
      <div style="text-align: center; padding: 48px; color: #888;">
        <mat-icon style="font-size: 48px; width: 48px; height: 48px;">inventory_2</mat-icon>
        <p>No products found.</p>
      </div>
    } @else {
      <table mat-table [dataSource]="products" class="full-width">
        <ng-container matColumnDef="sku">
          <th mat-header-cell *matHeaderCellDef>SKU</th>
          <td mat-cell *matCellDef="let product">{{ product.sku }}</td>
        </ng-container>

        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>Name</th>
          <td mat-cell *matCellDef="let product">{{ product.name }}</td>
        </ng-container>

        <ng-container matColumnDef="categoryId">
          <th mat-header-cell *matHeaderCellDef>Category</th>
          <td mat-cell *matCellDef="let product">{{ product.categoryId }}</td>
        </ng-container>

        <ng-container matColumnDef="unit">
          <th mat-header-cell *matHeaderCellDef>Unit</th>
          <td mat-cell *matCellDef="let product">{{ product.unit }}</td>
        </ng-container>

        <ng-container matColumnDef="minStock">
          <th mat-header-cell *matHeaderCellDef>Min Stock</th>
          <td mat-cell *matCellDef="let product">{{ product.minStock }}</td>
        </ng-container>

        <ng-container matColumnDef="reorderPoint">
          <th mat-header-cell *matHeaderCellDef>Reorder Point</th>
          <td mat-cell *matCellDef="let product">{{ product.reorderPoint }}</td>
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
            <a mat-icon-button [routerLink]="['/products', product.id, 'edit']"><mat-icon>edit</mat-icon></a>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
      </table>
    }

    <mat-paginator [length]="totalElements" [pageSize]="pageSize" [pageSizeOptions]="[10, 20, 50]"
                   (page)="onPageChange($event)"></mat-paginator>
  `
})
export class ProductListComponent implements OnInit {
  products: ProductSummary[] = [];
  displayedColumns = ['sku', 'name', 'categoryId', 'unit', 'minStock', 'reorderPoint', 'active', 'actions'];
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  loading = false;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private productService: ProductService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
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

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }
}
