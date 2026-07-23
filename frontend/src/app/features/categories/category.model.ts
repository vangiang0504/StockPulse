export interface Category {
  id: number;
  name: string;
  code: string;
  parentId?: number | null;
  createdAt: string;
}

export interface CreateCategoryRequest {
  name: string;
  code: string;
  parentId?: number;
}

export interface UpdateCategoryRequest {
  name?: string;
  parentId?: number;
}
