import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../core/models/api-response.model';
import { PageResponse } from '../../core/models/page-response.model';
import { Product, ProductSummary, CreateProductRequest, UpdateProductRequest } from './product.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiUrl = `${environment.apiUrl}/products`;

  constructor(private http: HttpClient) {}

  getProducts(page = 0, size = 20): Observable<ApiResponse<PageResponse<ProductSummary>>> {
    return this.http.get<ApiResponse<PageResponse<ProductSummary>>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  /** Full-text search over SKU and name (backend requires a non-blank query). */
  searchProducts(query: string, page = 0, size = 20): Observable<ApiResponse<PageResponse<ProductSummary>>> {
    const params = new HttpParams()
      .set('q', query)
      .set('page', page)
      .set('size', size);
    return this.http.get<ApiResponse<PageResponse<ProductSummary>>>(`${this.apiUrl}/search`, { params });
  }

  getProductById(id: number): Observable<ApiResponse<Product>> {
    return this.http.get<ApiResponse<Product>>(`${this.apiUrl}/${id}`);
  }

  createProduct(product: CreateProductRequest): Observable<ApiResponse<Product>> {
    return this.http.post<ApiResponse<Product>>(this.apiUrl, product);
  }

  updateProduct(id: number, product: UpdateProductRequest): Observable<ApiResponse<Product>> {
    return this.http.put<ApiResponse<Product>>(`${this.apiUrl}/${id}`, product);
  }
}
