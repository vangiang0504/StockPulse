import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiResponse } from '../../core/models/api-response.model';
import { PageResponse } from '../../core/models/page-response.model';
import { Warehouse } from './warehouse.model';
import { WarehouseService } from './warehouse.service';

describe('WarehouseService', () => {
  let service: WarehouseService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WarehouseService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(WarehouseService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the selected server page with typed pagination parameters', () => {
    let result: ApiResponse<PageResponse<Warehouse>> | undefined;
    service.getWarehouses(2, 50, 'code', 'DESC').subscribe(response => result = response);

    const request = http.expectOne(req => req.url === '/api/v1/warehouses');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('50');
    expect(request.request.params.get('sortBy')).toBe('code');
    expect(request.request.params.get('sortDir')).toBe('DESC');
    request.flush(pageResponse([warehouse]));

    expect(result?.data.content).toEqual([warehouse]);
  });

  it('uses stable list defaults', () => {
    service.getWarehouses().subscribe();

    const request = http.expectOne(req => req.url === '/api/v1/warehouses');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.get('sortBy')).toBe('name');
    expect(request.request.params.get('sortDir')).toBe('ASC');
    request.flush(pageResponse([]));
  });

  it('propagates an HTTP error response', () => {
    let status: number | undefined;
    service.getWarehouses().subscribe({ error: error => status = error.status });

    const request = http.expectOne(req => req.url === '/api/v1/warehouses');
    request.flush(
      { success: false, message: 'Warehouse service unavailable', data: null },
      { status: 503, statusText: 'Unavailable' }
    );

    expect(status).toBe(503);
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
