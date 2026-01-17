import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { City, CreateCityRequest } from '../models/city.model';

@Injectable({
  providedIn: 'root'
})
export class CityService {
  private api = inject(ApiService);

  async getCities(): Promise<City[]> {
    return this.api.get<City[]>('/api/cities');
  }

  async createCity(request: CreateCityRequest): Promise<City> {
    return this.api.post<City>('/api/cities', request);
  }

  async deleteCity(id: number): Promise<void> {
    return this.api.delete<void>(`/api/cities/${id}`);
  }

  async setActive(id: number, active: boolean): Promise<City> {
    return this.api.patch<City>(`/api/cities/${id}/active?active=${active}`);
  }
}
