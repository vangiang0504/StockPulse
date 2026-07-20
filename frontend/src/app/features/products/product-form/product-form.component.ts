import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ProductService } from '../product.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSlideToggleModule],
  template: `
    <h2>{{ isEdit ? 'Edit Product' : 'Create Product' }}</h2>

    <form [formGroup]="form" (ngSubmit)="onSubmit()" style="max-width: 500px;">
      <mat-form-field class="full-width">
        <mat-label>SKU</mat-label>
        <input matInput formControlName="sku">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Name</mat-label>
        <input matInput formControlName="name">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Description</mat-label>
        <textarea matInput formControlName="description" rows="3"></textarea>
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Category ID</mat-label>
        <input matInput type="number" formControlName="categoryId">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Unit</mat-label>
        <input matInput formControlName="unit">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Min Stock</mat-label>
        <input matInput type="number" formControlName="minStock">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Max Stock</mat-label>
        <input matInput type="number" formControlName="maxStock">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Reorder Point</mat-label>
        <input matInput type="number" formControlName="reorderPoint">
      </mat-form-field>

      <mat-form-field class="full-width">
        <mat-label>Reorder Quantity</mat-label>
        <input matInput type="number" formControlName="reorderQuantity">
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
  `
})
export class ProductFormComponent implements OnInit {
  isEdit = false;
  loading = false;
  productId: number | null = null;

  form = this.fb.group({
    sku: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    categoryId: [null as number | null, Validators.required],
    unit: ['PCS', [Validators.required, Validators.maxLength(20)]],
    minStock: [10, [Validators.required, Validators.min(0)]],
    maxStock: [1000, [Validators.required, Validators.min(0)]],
    reorderPoint: [20, [Validators.required, Validators.min(0)]],
    reorderQuantity: [100, [Validators.required, Validators.min(1)]],
    active: [true]
  });

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private route: ActivatedRoute,
    private router: Router,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.productId = +id;
      this.form.get('sku')?.disable();
      this.loadProduct();
    }
  }

  loadProduct(): void {
    this.productService.getProductById(this.productId!).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.form.patchValue({
            sku: res.data.sku,
            name: res.data.name,
            description: res.data.description,
            categoryId: res.data.categoryId,
            unit: res.data.unit,
            minStock: res.data.minStock,
            maxStock: res.data.maxStock,
            reorderPoint: res.data.reorderPoint,
            reorderQuantity: res.data.reorderQuantity,
            active: res.data.active
          });
        }
      },
      error: () => this.notification.error('Failed to load product')
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;

    if (this.isEdit) {
      const { name, description, categoryId, unit, minStock, maxStock, reorderPoint, reorderQuantity, active } = this.form.getRawValue();
      this.productService.updateProduct(this.productId!, {
        name: name!, description: description!, categoryId: categoryId!,
        unit: unit!, minStock: minStock!, maxStock: maxStock!,
        reorderPoint: reorderPoint!, reorderQuantity: reorderQuantity!, active: active!
      }).subscribe({
        next: () => {
          this.notification.success('Product updated');
          this.router.navigate(['/products']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Update failed');
        }
      });
    } else {
      const { sku, name, description, categoryId, unit, minStock, maxStock, reorderPoint, reorderQuantity } = this.form.getRawValue();
      this.productService.createProduct({
        sku: sku!, name: name!, description: description!, categoryId: categoryId!,
        unit: unit!, minStock: minStock!, maxStock: maxStock!,
        reorderPoint: reorderPoint!, reorderQuantity: reorderQuantity!
      }).subscribe({
        next: () => {
          this.notification.success('Product created');
          this.router.navigate(['/products']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Create failed');
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/products']);
  }
}
