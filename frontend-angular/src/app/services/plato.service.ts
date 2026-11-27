import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Page } from '../models/page';
import { Plato } from '../models/plato';

@Injectable({ providedIn: 'root' })
export class PlatoService {
  private apiUrl = 'http://localhost:8080/api/platos';

  constructor(private http: HttpClient) {}

  listarPaginado(page: number, size: number, criterio: string, texto: string,
                 idCategoria: number | null): Observable<Page<Plato>> {
    return this.http.get<Page<Plato>>(`${this.apiUrl}/paginado`, {
      params: {
        page,
        size,
        criterio,
        texto,
        idCategoria: idCategoria ?? ''
      }
    });
  }

  crear(plato: Plato): Observable<Plato> {
    return this.http.post<Plato>(this.apiUrl, plato);
  }

  actualizar(id: number, plato: Plato): Observable<Plato> {
    return this.http.put<Plato>(`${this.apiUrl}/${id}`, plato);
  }

  desactivar(id: number): Observable<Plato> {
    return this.http.delete<Plato>(`${this.apiUrl}/${id}`);
  }

  activar(id: number): Observable<Plato> {
    return this.http.patch<Plato>(`${this.apiUrl}/${id}/activar`, {});
  }
}
