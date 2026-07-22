export interface ProductSummary {
  id: number;
  sku: string;
  name: string;
  categoryId: number;
  unit: string;
  minStock: number;
  reorderPoint: number;
  active: boolean;
  createdAt: string;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  description?: string;
  categoryId: number;
  unit: string;
  minStock: number;
  maxStock: number;
  reorderPoint: number;
  reorderQuantity: number;
  active: boolean;
  createdAt: string;
}

export interface CreateProductRequest {
  sku: string;
  name: string;
  description?: string;
  categoryId: number;
  unit: string;
  minStock: number;
  maxStock: number;
  reorderPoint: number;
  reorderQuantity: number;
}

export interface UpdateProductRequest {
  name?: string;
  description?: string;
  categoryId?: number;
  unit?: string;
  minStock?: number;
  maxStock?: number;
  reorderPoint?: number;
  reorderQuantity?: number;
  active?: boolean;
}
