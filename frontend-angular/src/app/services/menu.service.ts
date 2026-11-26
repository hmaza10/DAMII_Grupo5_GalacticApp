import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { EstadoMenu, MenuSemanal, PlatoSeleccionable } from '../models/menu';
import { PageResponse } from '../models/paginacion';

@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly api = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  listar(page: number, size: number, estado: EstadoMenu | ''): Observable<PageResponse<MenuSemanal>> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));

    if (estado) {
      params = params.set('estado', estado);
    }

    return this.http.get<PageResponse<MenuSemanal>>(`${this.api}/menus/paginado`, { params });
  }

  obtener(id: number): Observable<MenuSemanal> {
    return this.http.get<MenuSemanal>(`${this.api}/menus/${id}`);
  }

  crear(menu: MenuSemanal): Observable<MenuSemanal> {
    return this.http.post<MenuSemanal>(`${this.api}/menus`, menu);
  }

  actualizar(id: number, menu: MenuSemanal): Observable<MenuSemanal> {
    return this.http.put<MenuSemanal>(`${this.api}/menus/${id}`, menu);
  }

  publicar(id: number): Observable<MenuSemanal> {
    return this.http.patch<MenuSemanal>(`${this.api}/menus/${id}/publicar`, null);
  }

  listarPlatos(): Observable<PlatoSeleccionable[]> {
    return this.http.get<PlatoSeleccionable[]>(`${this.api}/platos`);
  }
}
