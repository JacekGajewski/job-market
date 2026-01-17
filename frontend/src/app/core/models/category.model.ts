export interface Category {
  id: number;
  name: string;
  slug: string;
  active: boolean;
}

export interface CreateCategoryRequest {
  name: string;
  slug: string;
}
