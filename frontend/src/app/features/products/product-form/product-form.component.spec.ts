import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSelect } from '@angular/material/select';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, Subject, throwError } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { NotificationService } from '../../../core/services/notification.service';
import { Category } from '../../categories/category.model';
import { CategoryService } from '../../categories/category.service';
import { Product } from '../product.model';
import { ProductService } from '../product.service';
import { ProductFormComponent } from './product-form.component';

describe('ProductFormComponent', () => {
  let fixture: ComponentFixture<ProductFormComponent>;
  let component: ProductFormComponent;
  let productService: jasmine.SpyObj<ProductService>;
  let categoryService: jasmine.SpyObj<CategoryService>;
  let notification: jasmine.SpyObj<NotificationService>;
  let router: jasmine.SpyObj<Router>;
  let routeId: string | null;

  const firstPageCategory = category(3, 'Accessories', 'ACC');
  const laterPageCategory = category(9, 'Phones', 'PHONE');
  const product = productDetail(42, 9);

  beforeEach(async () => {
    routeId = null;
    productService = jasmine.createSpyObj<ProductService>(
      'ProductService',
      ['getProductById', 'createProduct', 'updateProduct']
    );
    categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', ['getAllCategories']);
    notification = jasmine.createSpyObj<NotificationService>('NotificationService', ['success', 'error']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    categoryService.getAllCategories.and.returnValue(of([firstPageCategory, laterPageCategory]));
    productService.getProductById.and.returnValue(of(apiResponse(product)));
    productService.createProduct.and.returnValue(of(apiResponse(product)));
    productService.updateProduct.and.returnValue(of(apiResponse(product)));

    await TestBed.configureTestingModule({
      imports: [ProductFormComponent, NoopAnimationsModule],
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: CategoryService, useValue: categoryService },
        { provide: NotificationService, useValue: notification },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => routeId } } }
        }
      ]
    }).compileComponents();
  });

  it('shows loading and prevents submission while Category options are pending', () => {
    categoryService.getAllCategories.and.returnValue(new Subject<Category[]>());
    createComponent();

    expect(component.categoryState).toBe('loading');
    expect(text()).toContain('Loading categories...');
    expect(submitButton().disabled).toBeTrue();

    component.onSubmit();
    expect(productService.createProduct).not.toHaveBeenCalled();
  });

  it('renders named options, keeps Category required, and sends a numeric create payload ID', () => {
    createComponent();

    expect(text()).toContain('Category');
    expect(component.form.controls.categoryId.hasError('required')).toBeTrue();

    component.form.patchValue({
      sku: 'SKU-100',
      name: 'New Product',
      description: 'Description',
      categoryId: laterPageCategory.id,
      unit: 'PCS',
      minStock: 10,
      maxStock: 100,
      reorderPoint: 20,
      reorderQuantity: 30
    });
    component.onSubmit();

    const request = productService.createProduct.calls.mostRecent().args[0];
    expect(request).toEqual({
      sku: 'SKU-100',
      name: 'New Product',
      description: 'Description',
      categoryId: 9,
      unit: 'PCS',
      minStock: 10,
      maxStock: 100,
      reorderPoint: 20,
      reorderQuantity: 30
    });
    expect(typeof request.categoryId).toBe('number');
    expect(notification.success).toHaveBeenCalledWith('Product created');
    expect(router.navigate).toHaveBeenCalledWith(['/products']);
  });

  it('preselects the exact numeric edit Category after all pages are available and preserves update behavior', async () => {
    routeId = '42';
    createComponent();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(categoryService.getAllCategories).toHaveBeenCalledTimes(1);
    expect(productService.getProductById).toHaveBeenCalledOnceWith(42);
    expect(component.form.controls.categoryId.value).toBe(9);
    expect(typeof component.form.controls.categoryId.value).toBe('number');
    expect(component.form.controls.sku.disabled).toBeTrue();
    const categorySelect = fixture.debugElement.query(By.directive(MatSelect)).componentInstance as MatSelect;
    expect(categorySelect.triggerValue).toBe('Phones (PHONE)');

    component.onSubmit();

    const request = productService.updateProduct.calls.mostRecent().args[1];
    expect(request.categoryId).toBe(9);
    expect(request.name).toBe(product.name);
    expect(productService.updateProduct.calls.mostRecent().args[0]).toBe(42);
    expect(notification.success).toHaveBeenCalledWith('Product updated');
  });

  it('shows an empty state and prevents submission when no Categories exist', () => {
    categoryService.getAllCategories.and.returnValue(of([]));
    createComponent();

    expect(component.categoryState).toBe('empty');
    expect(text()).toContain('No categories are available.');
    expect(submitButton().disabled).toBeTrue();
  });

  it('shows and reports a stable Category error without retaining partial options', () => {
    categoryService.getAllCategories.and.returnValue(throwError(() => new Error('network')));
    createComponent();

    expect(component.categoryState).toBe('error');
    expect(component.categories).toEqual([]);
    expect(text()).toContain('Categories could not be loaded.');
    expect(notification.error).toHaveBeenCalledOnceWith('Failed to load categories');
    expect(submitButton().disabled).toBeTrue();
  });

  it('blocks edit submission when the Product Category is absent from the completed option set', () => {
    routeId = '42';
    categoryService.getAllCategories.and.returnValue(of([firstPageCategory]));
    createComponent();

    expect(component.categoryState).toBe('error');
    expect(component.referenceDataReady).toBeFalse();
    expect(notification.error).toHaveBeenCalledOnceWith('Product category is unavailable');
    expect(submitButton().disabled).toBeTrue();
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(ProductFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function submitButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
  }

  function category(id: number, name: string, code: string): Category {
    return { id, name, code, parentId: null, createdAt: '2026-07-22T00:00:00' };
  }

  function productDetail(id: number, categoryId: number): Product {
    return {
      id,
      sku: 'SKU-EDIT',
      name: 'Existing Product',
      description: 'Existing description',
      categoryId,
      unit: 'PCS',
      minStock: 10,
      maxStock: 100,
      reorderPoint: 20,
      reorderQuantity: 30,
      active: true,
      createdAt: '2026-07-22T00:00:00'
    };
  }

  function apiResponse(data: Product): ApiResponse<Product> {
    return {
      success: true,
      message: 'OK',
      data,
      timestamp: '2026-07-22T00:00:00'
    };
  }
});
