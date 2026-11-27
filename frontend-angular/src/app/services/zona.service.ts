import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Page } from '../models/page';
import { Zona } from '../models/zona';

@Injectable({ providedIn: 'root' })
export class ZonaService {
  private apiUrl = 'http://localhost:8080/api/zonas';

  constructor(private http: HttpClient) {}

  listarPaginado(page: number, size: number, texto: string): Observable<Page<Zona>> {
    return this.http.get<Page<Zona>>(`${this.apiUrl}/paginado`, {
      params: { page, size, texto }
    });
  }

  crear(zona: Zona): Observable<Zona> {
    return this.http.post<Zona>(this.apiUrl, zona);
  }

  actualizar(id: number, zona: Zona): Observable<Zona> {
    return this.http.put<Zona>(`${this.apiUrl}/${id}`, zona);
  }

  desactivar(id: number): Observable<Zona> {
    return this.http.delete<Zona>(`${this.apiUrl}/${id}`);
  }

  activar(id: number): Observable<Zona> {
    return this.http.patch<Zona>(`${this.apiUrl}/${id}/activar`, {});
  }
}
