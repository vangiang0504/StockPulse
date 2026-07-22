import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiResponse } from '../../core/models/api-response.model';
import { PageResponse } from '../../core/models/page-response.model';
import { Category } from './category.model';
import { CategoryService } from './category.service';

describe('CategoryService', () => {
  let service: CategoryService;
  let http: HttpTestingController;

  const hardware = category(1, 'Hardware', 'HW');
  const phones = category(2, 'Phones', 'PHONE');

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CategoryService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CategoryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the typed paginated Category endpoint parameters', () => {
    let result: ApiResponse<PageResponse<Category>> | undefined;
    service.getCategories(2, 25, 'name', 'DESC').subscribe(response => result = response);

    const request = http.expectOne(req => req.url === '/api/v1/categories');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('25');
    expect(request.request.params.get('sortBy')).toBe('name');
    expect(request.request.params.get('sortDir')).toBe('DESC');
    request.flush(page([hardware], 2, 3, true));

    expect(result?.data.content[0]).toEqual(hardware);
  });

  it('loads all zero-based pages from metadata, de-duplicates IDs, and sorts deterministically', () => {
    let result: Category[] | undefined;
    service.getAllCategories().subscribe(categories => result = categories);

    const first = expectAllRequest(0);
    first.flush(page([phones], 0, 2, false));
    const second = expectAllRequest(1);
    second.flush(page([hardware, phones], 1, 2, true));

    expect(result).toEqual([hardware, phones]);
  });

  it('rejects an unsuccessful or missing-data envelope instead of returning partial options', () => {
    let error: unknown;
    service.getAllCategories().subscribe({ error: value => error = value });

    expectAllRequest(0).flush({
      success: false,
      message: 'failed',
      data: null,
      timestamp: '2026-07-22T00:00:00'
    });

    expect(error).toEqual(jasmine.any(Error));
  });

  it('rejects inconsistent pagination metadata', () => {
    let error: unknown;
    service.getAllCategories().subscribe({ error: value => error = value });

    expectAllRequest(0).flush(page([hardware], 0, 2, true));

    expect(error).toEqual(jasmine.any(Error));
  });

  it('propagates an HTTP failure', () => {
    let error: unknown;
    service.getAllCategories().subscribe({ error: value => error = value });

    expectAllRequest(0).flush('network', { status: 503, statusText: 'Unavailable' });

    expect(error).toBeDefined();
  });

  function expectAllRequest(expectedPage: number) {
    const request = http.expectOne(req => req.url === '/api/v1/categories' && req.params.get('page') === `${expectedPage}`);
    expect(request.request.params.get('size')).toBe('50');
    expect(request.request.params.get('sortBy')).toBe('name');
    expect(request.request.params.get('sortDir')).toBe('ASC');
    return request;
  }

  function page(
    content: Category[],
    pageNumber: number,
    totalPages: number,
    last: boolean
  ): ApiResponse<PageResponse<Category>> {
    return {
      success: true,
      message: 'OK',
      timestamp: '2026-07-22T00:00:00',
      data: {
        content,
        page: pageNumber,
        size: 50,
        totalElements: content.length,
        totalPages,
        last
      }
    };
  }

  function category(id: number, name: string, code: string): Category {
    return { id, name, code, parentId: null, createdAt: '2026-07-22T00:00:00' };
  }
});
