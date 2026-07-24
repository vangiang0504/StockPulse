import { Component, OnInit } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { NotificationService } from '../../../core/services/notification.service';
import { Warehouse } from '../warehouse.model';
import { WarehouseService } from '../warehouse.service';

@Component({
  selector: 'app-warehouse-list',
  standalone: true,
  imports: [MatTableModule, MatPaginatorModule, MatChipsModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h2>Warehouses</h2>
          <p class="page-subtitle">Storage locations across the network</p>
        </div>
      </div>

      @if (loading) {
        <div class="state" aria-live="polite">
          <mat-spinner diameter="40"></mat-spinner>
          <span class="visually-hidden">Loading warehouses</span>
        </div>
      } @else if (errorMessage) {
        <div class="state error-state" role="alert">
          <mat-icon>error_outline</mat-icon>
          <p>{{ errorMessage }}</p>
        </div>
      } @else if (warehouses.length === 0) {
        <div class="state empty-state">
          <mat-icon>warehouse</mat-icon>
          <p>No warehouses found.</p>
        </div>
      } @else {
        <div class="surface-card">
          <table mat-table [dataSource]="warehouses" class="full-width">
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Name</th>
              <td mat-cell *matCellDef="let warehouse"><span class="cell-strong">{{ warehouse.name }}</span></td>
            </ng-container>

            <ng-container matColumnDef="code">
              <th mat-header-cell *matHeaderCellDef>Code</th>
              <td mat-cell *matCellDef="let warehouse"><span class="sku-chip">{{ warehouse.code }}</span></td>
            </ng-container>

            <ng-container matColumnDef="address">
              <th mat-header-cell *matHeaderCellDef>Address</th>
              <td mat-cell *matCellDef="let warehouse">{{ warehouse.address }}</td>
            </ng-container>

            <ng-container matColumnDef="active">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let warehouse">
                <mat-chip [highlighted]="warehouse.active">
                  {{ warehouse.active ? 'Active' : 'Inactive' }}
                </mat-chip>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          <mat-paginator
            [length]="totalElements"
            [pageIndex]="currentPage"
            [pageSize]="pageSize"
            [pageSizeOptions]="pageSizeOptions"
            (page)="onPageChange($event)">
          </mat-paginator>
        </div>
      }
    </div>
  `,
  styles: [`
    .state { min-height: 200px; display: flex; flex-direction: column; align-items: center; justify-content: center;
      gap: 8px; color: var(--sp-text-faint);
      background: var(--sp-surface); border: 1px solid var(--sp-border); border-radius: var(--sp-radius); }
    .state mat-icon { font-size: 48px; width: 48px; height: 48px; }
    .error-state { color: #b00020; }
    .cell-strong { font-weight: 500; color: var(--sp-text); }
    .visually-hidden { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
  `]
})
export class WarehouseListComponent implements OnInit {
  readonly displayedColumns = ['name', 'code', 'address', 'active'];
  readonly pageSizeOptions = [10, 20, 50];

  warehouses: Warehouse[] = [];
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  loading = false;
  errorMessage = '';

  constructor(
    private warehouseService: WarehouseService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
  }

  loadWarehouses(): void {
    this.loading = true;
    this.errorMessage = '';

    this.warehouseService.getWarehouses(this.currentPage, this.pageSize).subscribe({
      next: response => {
        if (!response?.success || !response.data || !Array.isArray(response.data.content)) {
          this.showLoadError();
          return;
        }

        this.warehouses = response.data.content;
        this.totalElements = response.data.totalElements;
        this.loading = false;
      },
      error: () => this.showLoadError()
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadWarehouses();
  }

  private showLoadError(): void {
    this.loading = false;
    this.warehouses = [];
    this.totalElements = 0;
    this.errorMessage = 'Failed to load warehouses.';
    this.notification.error('Failed to load warehouses');
  }
}
