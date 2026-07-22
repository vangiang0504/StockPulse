import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../core/models/api-response.model';
import { PageResponse } from '../../core/models/page-response.model';
import { Warehouse, CreateWarehouseRequest, UpdateWarehouseRequest } from './warehouse.model';

@Injectable({
  providedIn: 'root'
})
export class WarehouseService {
  private apiUrl = `${environment.apiUrl}/warehouses`;

  constructor(private http: HttpClient) {}

  getWarehouses(page = 0, size = 20): Observable<ApiResponse<PageResponse<Warehouse>>> {
    return this.http.get<ApiResponse<PageResponse<Warehouse>>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  getWarehouseById(id: number): Observable<ApiResponse<Warehouse>> {
    return this.http.get<ApiResponse<Warehouse>>(`${this.apiUrl}/${id}`);
  }

  createWarehouse(warehouse: CreateWarehouseRequest): Observable<ApiResponse<Warehouse>> {
    return this.http.post<ApiResponse<Warehouse>>(this.apiUrl, warehouse);
  }

  updateWarehouse(id: number, warehouse: UpdateWarehouseRequest): Observable<ApiResponse<Warehouse>> {
    return this.http.put<ApiResponse<Warehouse>>(`${this.apiUrl}/${id}`, warehouse);
  }
}
