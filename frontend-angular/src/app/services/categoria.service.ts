import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Categoria } from '../models/categoria';
import { Page } from '../models/page';

@Injectable({ providedIn: 'root' })
export class CategoriaService {
  private apiUrl = 'http://localhost:8080/api/categorias';

  constructor(private http: HttpClient) {}

  listarPaginado(page: number, size: number, texto: string): Observable<Page<Categoria>> {
    return this.http.get<Page<Categoria>>(`${this.apiUrl}/paginado`, {
      params: { page, size, texto }
    });
  }

  crear(categoria: Categoria): Observable<Categoria> {
    return this.http.post<Categoria>(this.apiUrl, categoria);
  }

  actualizar(id: number, categoria: Categoria): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.apiUrl}/${id}`, categoria);
  }

  desactivar(id: number): Observable<Categoria> {
    return this.http.delete<Categoria>(`${this.apiUrl}/${id}`);
  }

  activar(id: number): Observable<Categoria> {
    return this.http.patch<Categoria>(`${this.apiUrl}/${id}/activar`, {});
  }
}
