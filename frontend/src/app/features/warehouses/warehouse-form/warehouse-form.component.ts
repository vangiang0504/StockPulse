import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { WarehouseService } from '../warehouse.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-warehouse-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSlideToggleModule],
  template: `
    <div class="page">
      <div class="page-head">
        <div>
          <h2>{{ isEdit ? 'Edit Warehouse' : 'Create Warehouse' }}</h2>
          <p class="page-subtitle">Storage location details</p>
        </div>
      </div>
      <div class="surface-card pad form-card">
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <mat-form-field class="full-width">
            <mat-label>Name</mat-label>
            <input matInput formControlName="name">
          </mat-form-field>

          <mat-form-field class="full-width">
            <mat-label>Code</mat-label>
            <input matInput formControlName="code">
            @if (isEdit) {
              <mat-hint>Code is immutable and cannot be changed.</mat-hint>
            }
          </mat-form-field>

          <mat-form-field class="full-width">
            <mat-label>Address</mat-label>
            <textarea matInput formControlName="address" rows="2"></textarea>
          </mat-form-field>

          @if (isEdit) {
            <mat-slide-toggle formControlName="active">Active</mat-slide-toggle>
          }

          <div style="margin-top: 24px; display: flex; gap: 8px;">
            <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">
              {{ loading ? 'Saving...' : (isEdit ? 'Update' : 'Create') }}
            </button>
            <button mat-button type="button" (click)="onCancel()">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .form-card { max-width: 560px; }
  `]
})
export class WarehouseFormComponent implements OnInit {
  isEdit = false;
  loading = false;
  warehouseId: number | null = null;

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    code: ['', [Validators.required, Validators.maxLength(50)]],
    address: ['', [Validators.maxLength(500)]],
    active: [true]
  });

  constructor(
    private fb: FormBuilder,
    private warehouseService: WarehouseService,
    private route: ActivatedRoute,
    private router: Router,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.warehouseId = +id;
      this.form.get('code')?.disable(); // business key is immutable
      this.loadWarehouse();
    }
  }

  private loadWarehouse(): void {
    this.warehouseService.getWarehouseById(this.warehouseId!).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.form.patchValue({
            name: res.data.name,
            code: res.data.code,
            address: res.data.address ?? '',
            active: res.data.active
          });
        } else {
          this.notification.error('Failed to load warehouse');
        }
      },
      error: () => this.notification.error('Failed to load warehouse')
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;

    if (this.isEdit) {
      const { name, address, active } = this.form.getRawValue();
      this.warehouseService.updateWarehouse(this.warehouseId!, {
        name: name!, address: address ?? undefined, active: active!
      }).subscribe({
        next: () => {
          this.notification.success('Warehouse updated');
          this.router.navigate(['/warehouses']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Update failed');
        }
      });
    } else {
      const { name, code, address } = this.form.getRawValue();
      this.warehouseService.createWarehouse({
        name: name!, code: code!, address: address ?? undefined
      }).subscribe({
        next: () => {
          this.notification.success('Warehouse created');
          this.router.navigate(['/warehouses']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Create failed');
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/warehouses']);
  }
}
