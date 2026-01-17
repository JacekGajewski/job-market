import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Category, CreateCategoryRequest } from '../models/category.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private api = inject(ApiService);

  async getCategories(): Promise<Category[]> {
    return this.api.get<Category[]>('/api/categories');
  }

  async createCategory(request: CreateCategoryRequest): Promise<Category> {
    return this.api.post<Category>('/api/categories', request);
  }

  async deleteCategory(id: number): Promise<void> {
    return this.api.delete<void>(`/api/categories/${id}`);
  }
}
