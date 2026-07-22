import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { EMPTY, Observable, expand, map, reduce } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../core/models/api-response.model';
import { PageResponse } from '../../core/models/page-response.model';
import { Category, CreateCategoryRequest, UpdateCategoryRequest } from './category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly apiUrl = `${environment.apiUrl}/categories`;

  constructor(private http: HttpClient) {}

  getCategories(
    page = 0,
    size = 50,
    sortBy = 'name',
    sortDir = 'ASC'
  ): Observable<ApiResponse<PageResponse<Category>>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<ApiResponse<PageResponse<Category>>>(this.apiUrl, { params });
  }

  getAllCategories(): Observable<Category[]> {
    const pageSize = 50;
    const sortBy = 'name';
    const sortDir = 'ASC';

    return this.loadPage(0, pageSize, sortBy, sortDir).pipe(
      expand(page => {
        if (page.last || page.totalPages === 0) {
          return EMPTY;
        }
        return this.loadPage(page.page + 1, pageSize, sortBy, sortDir);
      }),
      reduce((categoriesById, page) => {
        for (const category of page.content) {
          categoriesById.set(category.id, category);
        }
        return categoriesById;
      }, new Map<number, Category>()),
      map(categoriesById => Array.from(categoriesById.values()).sort((left, right) =>
        left.name.localeCompare(right.name) || left.code.localeCompare(right.code) || left.id - right.id
      ))
    );
  }

  getCategoryById(id: number): Observable<ApiResponse<Category>> {
    return this.http.get<ApiResponse<Category>>(`${this.apiUrl}/${id}`);
  }

  createCategory(category: CreateCategoryRequest): Observable<ApiResponse<Category>> {
    return this.http.post<ApiResponse<Category>>(this.apiUrl, category);
  }

  updateCategory(id: number, category: UpdateCategoryRequest): Observable<ApiResponse<Category>> {
    return this.http.put<ApiResponse<Category>>(`${this.apiUrl}/${id}`, category);
  }

  private loadPage(
    expectedPage: number,
    size: number,
    sortBy: string,
    sortDir: string
  ): Observable<PageResponse<Category>> {
    return this.getCategories(expectedPage, size, sortBy, sortDir).pipe(
      map(response => {
        const page = response?.data;
        if (!response?.success || !page || !Array.isArray(page.content)) {
          throw new Error('Invalid Category response');
        }
        if (page.page !== expectedPage || page.totalPages < 0) {
          throw new Error('Inconsistent Category pagination');
        }
        const expectedLast = page.totalPages === 0 || page.page + 1 >= page.totalPages;
        if (page.last !== expectedLast) {
          throw new Error('Inconsistent Category pagination');
        }
        return page;
      })
    );
  }
}
