import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageEvent } from '@angular/material/paginator';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { PageResponse } from '../../../core/models/page-response.model';
import { NotificationService } from '../../../core/services/notification.service';
import { Warehouse } from '../warehouse.model';
import { WarehouseService } from '../warehouse.service';
import { AuthService } from '../../../core/services/auth.service';
import { WarehouseListComponent } from './warehouse-list.component';

describe('WarehouseListComponent', () => {
  let fixture: ComponentFixture<WarehouseListComponent>;
  let component: WarehouseListComponent;
  let warehouseService: jasmine.SpyObj<WarehouseService>;
  let notification: jasmine.SpyObj<NotificationService>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    warehouseService = jasmine.createSpyObj<WarehouseService>('WarehouseService', ['getWarehouses']);
    notification = jasmine.createSpyObj<NotificationService>('NotificationService', ['error']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getRole']);
    warehouseService.getWarehouses.and.returnValue(of(pageResponse([warehouse])));
    authService.getRole.and.returnValue('ADMIN');

    await TestBed.configureTestingModule({
      imports: [WarehouseListComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: WarehouseService, useValue: warehouseService },
        { provide: NotificationService, useValue: notification },
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WarehouseListComponent);
    component = fixture.componentInstance;
  });

  it('shows a loading state while the server request is pending', () => {
    warehouseService.getWarehouses.and.returnValue(new Subject<ApiResponse<PageResponse<Warehouse>>>());

    fixture.detectChanges();

    expect(component.loading).toBeTrue();
    expect(fixture.nativeElement.querySelector('mat-spinner')).not.toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Loading warehouses');
  });

  it('shows an empty state for an empty server page', () => {
    warehouseService.getWarehouses.and.returnValue(of(pageResponse([])));

    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No warehouses found.');
  });

  it('renders Name, Code, Address and Status content', () => {
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(component.displayedColumns).toEqual(['name', 'code', 'address', 'active']);
    expect(text).toContain('Name');
    expect(text).toContain('Main Warehouse');
    expect(text).toContain('WH-001');
    expect(text).toContain('123 Test Street');
    expect(text).toContain('Active');
  });

  it('shows an inline error state when the request fails', () => {
    warehouseService.getWarehouses.and.returnValue(throwError(() => new Error('network')));

    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Failed to load warehouses.');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
    expect(notification.error).toHaveBeenCalledOnceWith('Failed to load warehouses');
  });

  it('treats an unsuccessful response envelope as an error', () => {
    warehouseService.getWarehouses.and.returnValue(of({
      success: false,
      message: 'Failed',
      data: null as unknown as PageResponse<Warehouse>,
      timestamp: '2026-07-22T00:00:00'
    }));

    fixture.detectChanges();

    expect(component.errorMessage).toBe('Failed to load warehouses.');
  });

  it('requests the selected server page and supports page sizes 10, 20 and 50', () => {
    fixture.detectChanges();
    warehouseService.getWarehouses.calls.reset();

    component.onPageChange({ pageIndex: 3, pageSize: 50, length: 100 } as PageEvent);

    expect(component.pageSizeOptions).toEqual([10, 20, 50]);
    expect(warehouseService.getWarehouses).toHaveBeenCalledOnceWith(3, 50);
  });

  const warehouse: Warehouse = {
    id: 1,
    name: 'Main Warehouse',
    code: 'WH-001',
    address: '123 Test Street',
    active: true,
    createdAt: '2026-07-22T00:00:00'
  };

  function pageResponse(content: Warehouse[]): ApiResponse<PageResponse<Warehouse>> {
    return {
      success: true,
      message: 'OK',
      timestamp: '2026-07-22T00:00:00',
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
});
