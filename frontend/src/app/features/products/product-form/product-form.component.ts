import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSelectModule } from '@angular/material/select';
import { catchError, forkJoin, throwError } from 'rxjs';
import { ProductService } from '../product.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Category } from '../../categories/category.model';
import { CategoryService } from '../../categories/category.service';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSlideToggleModule, MatSelectModule],
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
        <mat-label>Category</mat-label>
        <mat-select formControlName="categoryId">
          @for (category of categories; track category.id) {
            <mat-option [value]="category.id">{{ category.name }} ({{ category.code }})</mat-option>
          }
        </mat-select>
      </mat-form-field>

      @if (categoryState === 'loading') {
        <p class="category-state" role="status">Loading categories...</p>
      } @else if (categoryState === 'empty') {
        <p class="category-state category-error" role="alert">No categories are available.</p>
      } @else if (categoryState === 'error') {
        <p class="category-state category-error" role="alert">Categories could not be loaded.</p>
      }

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
        <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading || !referenceDataReady">
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
  categories: Category[] = [];
  categoryState: 'loading' | 'ready' | 'empty' | 'error' = 'loading';
  private productReady = false;

  form = this.fb.group({
    sku: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    categoryId: [{ value: null as number | null, disabled: true }, Validators.required],
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
    private categoryService: CategoryService,
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
      this.loadEditData();
    } else {
      this.loadCategories();
    }
  }

  get referenceDataReady(): boolean {
    return this.categoryState === 'ready' && (!this.isEdit || this.productReady);
  }

  private loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: categories => this.setCategories(categories),
      error: () => this.failCategoryLoad()
    });
  }

  private loadEditData(): void {
    forkJoin({
      categories: this.categoryService.getAllCategories().pipe(
        catchError(() => throwError(() => new Error('categories')))
      ),
      product: this.productService.getProductById(this.productId!).pipe(
        catchError(() => throwError(() => new Error('product')))
      )
    }).subscribe({
      next: ({ categories, product }) => {
        this.setCategories(categories);
        if (this.categoryState !== 'ready') {
          return;
        }
        if (!product.success || !product.data) {
          this.notification.error('Failed to load product');
          return;
        }
        if (!categories.some(category => category.id === product.data!.categoryId)) {
          this.categoryState = 'error';
          this.form.controls.categoryId.disable();
          this.notification.error('Product category is unavailable');
          return;
        }

        this.form.patchValue({
          sku: product.data.sku,
          name: product.data.name,
          description: product.data.description,
          categoryId: product.data.categoryId,
          unit: product.data.unit,
          minStock: product.data.minStock,
          maxStock: product.data.maxStock,
          reorderPoint: product.data.reorderPoint,
          reorderQuantity: product.data.reorderQuantity,
          active: product.data.active
        });
        this.productReady = true;
      },
      error: (error: Error) => {
        if (error.message === 'product') {
          this.notification.error('Failed to load product');
        } else {
          this.failCategoryLoad();
        }
      }
    });
  }

  private setCategories(categories: Category[]): void {
    this.categories = categories;
    this.categoryState = categories.length > 0 ? 'ready' : 'empty';
    if (this.categoryState === 'ready') {
      this.form.controls.categoryId.enable();
    } else {
      this.form.controls.categoryId.disable();
    }
  }

  private failCategoryLoad(): void {
    this.categories = [];
    this.categoryState = 'error';
    this.form.controls.categoryId.disable();
    this.notification.error('Failed to load categories');
  }

  onSubmit(): void {
    if (this.form.invalid || !this.referenceDataReady) return;
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
