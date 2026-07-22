export interface Category {
  id: number;
  name: string;
  code: string;
  parentId: number | null;
  createdAt: string;
}
