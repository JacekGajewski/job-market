import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl;

  async get<T>(endpoint: string): Promise<T> {
    try {
      return await firstValueFrom(
        this.http.get<T>(`${this.baseUrl}${endpoint}`)
      );
    } catch (error) {
      this.handleError(error);
      throw error;
    }
  }

  async post<T>(endpoint: string, body: unknown): Promise<T> {
    try {
      return await firstValueFrom(
        this.http.post<T>(`${this.baseUrl}${endpoint}`, body)
      );
    } catch (error) {
      this.handleError(error);
      throw error;
    }
  }

  async delete<T>(endpoint: string): Promise<T> {
    try {
      return await firstValueFrom(
        this.http.delete<T>(`${this.baseUrl}${endpoint}`)
      );
    } catch (error) {
      this.handleError(error);
      throw error;
    }
  }

  async patch<T>(endpoint: string, body?: unknown): Promise<T> {
    try {
      return await firstValueFrom(
        this.http.patch<T>(`${this.baseUrl}${endpoint}`, body)
      );
    } catch (error) {
      this.handleError(error);
      throw error;
    }
  }

  private handleError(error: unknown): void {
    if (error instanceof HttpErrorResponse) {
      console.error(`API Error: ${error.status} - ${error.message}`);
    } else {
      console.error('Unknown error:', error);
    }
  }
}
