import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../core/models/api-response.model';
import { PageResponse } from '../../core/models/page-response.model';
import { Warehouse } from './warehouse.model';

@Injectable({ providedIn: 'root' })
export class WarehouseService {
  private readonly apiUrl = `${environment.apiUrl}/warehouses`;

  constructor(private http: HttpClient) {}

  getWarehouses(
    page = 0,
    size = 20,
    sortBy = 'name',
    sortDir = 'ASC'
  ): Observable<ApiResponse<PageResponse<Warehouse>>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<ApiResponse<PageResponse<Warehouse>>>(this.apiUrl, { params });
  }
}
