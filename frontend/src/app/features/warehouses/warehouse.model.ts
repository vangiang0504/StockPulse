export interface Warehouse {
  id: number;
  name: string;
  code: string;
  address?: string;
  active: boolean;
  createdAt: string;
}

export interface CreateWarehouseRequest {
  name: string;
  code: string;
  address?: string;
}

export interface UpdateWarehouseRequest {
  name?: string;
  address?: string;
  active?: boolean;
}
