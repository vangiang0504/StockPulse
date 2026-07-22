import { Component, OnInit } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WarehouseService } from '../warehouse.service';
import { Warehouse } from '../warehouse.model';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-warehouse-list',
  standalone: true,
  imports: [MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressSpinnerModule],
  template: `
    <div style="display: flex; justify-content: space-between; align-items: center;">
      <h2>Warehouses</h2>
    </div>

    @if (loading) {
      <div style="display: flex; justify-content: center; padding: 48px;">
        <mat-spinner diameter="40"></mat-spinner>
      </div>
    } @else if (warehouses.length === 0) {
      <div style="text-align: center; padding: 48px; color: #888;">
        <mat-icon style="font-size: 48px; width: 48px; height: 48px;">warehouse</mat-icon>
        <p>No warehouses found.</p>
      </div>
    } @else {
      <table mat-table [dataSource]="warehouses" class="full-width">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>Name</th>
          <td mat-cell *matCellDef="let warehouse">{{ warehouse.name }}</td>
        </ng-container>

        <ng-container matColumnDef="code">
          <th mat-header-cell *matHeaderCellDef>Code</th>
          <td mat-cell *matCellDef="let warehouse">{{ warehouse.code }}</td>
        </ng-container>

        <ng-container matColumnDef="address">
          <th mat-header-cell *matHeaderCellDef>Address</th>
          <td mat-cell *matCellDef="let warehouse">{{ warehouse.address || '—' }}</td>
        </ng-container>

        <ng-container matColumnDef="active">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let warehouse">
            <mat-chip [highlighted]="warehouse.active">{{ warehouse.active ? 'Active' : 'Inactive' }}</mat-chip>
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
export class WarehouseListComponent implements OnInit {
  warehouses: Warehouse[] = [];
  displayedColumns = ['name', 'code', 'address', 'active'];
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  loading = false;

  constructor(
    private warehouseService: WarehouseService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
  }

  loadWarehouses(): void {
    this.loading = true;
    this.warehouseService.getWarehouses(this.currentPage, this.pageSize).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.warehouses = res.data.content;
          this.totalElements = res.data.totalElements;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load warehouses');
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadWarehouses();
  }
}
