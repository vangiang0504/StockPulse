import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { PageResponse } from '../../../core/models/page-response.model';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductSummary } from '../product.model';
import { ProductService } from '../product.service';
import { CategoryService } from '../../categories/category.service';
import { Category } from '../../categories/category.model';
import { AuthService } from '../../../core/services/auth.service';
import { ProductListComponent } from './product-list.component';

describe('ProductListComponent', () => {
  let fixture: ComponentFixture<ProductListComponent>;
  let component: ProductListComponent;
  let productService: jasmine.SpyObj<ProductService>;
  let notification: jasmine.SpyObj<NotificationService>;
  let categoryService: jasmine.SpyObj<CategoryService>;
  let authService: jasmine.SpyObj<AuthService>;

  const product: ProductSummary = {
    id: 1,
    sku: 'SKU-001',
    name: 'Test Product',
    categoryId: 7,
    unit: 'PCS',
    minStock: 12,
    reorderPoint: 24,
    active: true,
    createdAt: '2026-07-21T00:00:00'
  };

  beforeEach(async () => {
    productService = jasmine.createSpyObj<ProductService>('ProductService', ['getProducts', 'searchProducts']);
    notification = jasmine.createSpyObj<NotificationService>('NotificationService', ['error']);
    categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', ['getCategories']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getRole']);
    productService.getProducts.and.returnValue(of(pageResponse([product])));
    productService.searchProducts.and.returnValue(of(pageResponse([product])));
    categoryService.getCategories.and.returnValue(of(categoryPage()));
    authService.getRole.and.returnValue('ADMIN');

    await TestBed.configureTestingModule({
      imports: [ProductListComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: ProductService, useValue: productService },
        { provide: NotificationService, useValue: notification },
        { provide: CategoryService, useValue: categoryService },
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductListComponent);
    component = fixture.componentInstance;
  });

  it('renders Min Stock and Reorder Point columns with their values', () => {
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(component.displayedColumns).toEqual([
      'sku', 'name', 'categoryId', 'unit', 'minStock', 'reorderPoint', 'active', 'actions'
    ]);
    expect(text).toContain('Min Stock');
    expect(text).toContain('Reorder Point');
    expect(text).toContain('12');
    expect(text).toContain('24');
  });

  it('keeps the loading state while the request is pending', () => {
    productService.getProducts.and.returnValue(new Subject<ApiResponse<PageResponse<ProductSummary>>>());

    fixture.detectChanges();

    expect(component.loading).toBeTrue();
    expect(fixture.nativeElement.querySelector('mat-spinner')).not.toBeNull();
  });

  it('keeps the empty state when the page contains no products', () => {
    productService.getProducts.and.returnValue(of(pageResponse([])));

    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No products found.');
  });

  it('requests the selected server-side page and page size', () => {
    fixture.detectChanges();
    productService.getProducts.calls.reset();

    component.onPageChange({ pageIndex: 2, pageSize: 50, length: 100 } as PageEvent);

    expect(productService.getProducts).toHaveBeenCalledOnceWith(2, 50);
  });

  it('keeps the established error notification behavior', () => {
    productService.getProducts.and.returnValue(throwError(() => new Error('network')));

    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(notification.error).toHaveBeenCalledOnceWith('Failed to load products');
  });

  it('calls the search endpoint (page reset) after the debounce, not getProducts', fakeAsync(() => {
    fixture.detectChanges();
    productService.getProducts.calls.reset();

    component.onSearch('mouse');
    tick(300);

    expect(productService.searchProducts).toHaveBeenCalledOnceWith('mouse', 0, 20);
    expect(productService.getProducts).not.toHaveBeenCalled();
  }));

  it('falls back to getProducts when the search box is cleared', fakeAsync(() => {
    fixture.detectChanges();
    component.onSearch('mouse');
    tick(300);
    productService.getProducts.calls.reset();

    component.clearSearch();
    tick(300);

    expect(productService.getProducts).toHaveBeenCalledOnceWith(0, 20);
  }));

  function pageResponse(content: ProductSummary[]): ApiResponse<PageResponse<ProductSummary>> {
    return {
      success: true,
      message: 'OK',
      timestamp: '2026-07-21T00:00:00',
      data: {
        content,
        page: 0,
        size: 20,
        totalElements: content.length,
        totalPages: content.length ? 1 : 0,
        last: true
      }
    };
  }

  function categoryPage(): ApiResponse<PageResponse<Category>> {
    return {
      success: true,
      message: 'OK',
      timestamp: '2026-07-21T00:00:00',
      data: { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0, last: true }
    };
  }
});
