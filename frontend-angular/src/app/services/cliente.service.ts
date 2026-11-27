import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Cliente } from '../models/cliente';
import { Page } from '../models/page';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private apiUrl = 'http://localhost:8080/api/clientes';

  constructor(private http: HttpClient) {}

  listarPaginado(page: number, size: number, criterio: string, texto: string,
                 idZona: number | null): Observable<Page<Cliente>> {
    return this.http.get<Page<Cliente>>(`${this.apiUrl}/paginado`, {
      params: {
        page,
        size,
        criterio,
        texto,
        idZona: idZona ?? ''
      }
    });
  }

  crear(cliente: Cliente): Observable<Cliente> {
    return this.http.post<Cliente>(this.apiUrl, cliente);
  }

  actualizar(id: number, cliente: Cliente): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.apiUrl}/${id}`, cliente);
  }

  desactivar(id: number): Observable<Cliente> {
    return this.http.delete<Cliente>(`${this.apiUrl}/${id}`);
  }

  activar(id: number): Observable<Cliente> {
    return this.http.patch<Cliente>(`${this.apiUrl}/${id}/activar`, {});
  }
}
