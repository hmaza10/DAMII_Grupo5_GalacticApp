import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

export interface CategoriaResumen {
  idCategoria: number;
  nombre: string;
}

export interface PlatoResumen {
  idPlato: number;
  nombre: string;
}

export interface ZonaResumen {
  idZona: number;
  nombre: string;
}

export interface PedidoResumen {
  idPedido: number;
  estado: string;
  total: number;
  fechaCreacion: string;
  cliente?: {
    usuario?: {
      nombres?: string;
      apellidos?: string;
    };
  };
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  listarCategorias(): Observable<CategoriaResumen[]> {
    return this.http.get<CategoriaResumen[]>(`${this.api}/categorias`);
  }

  listarPlatos(): Observable<PlatoResumen[]> {
    return this.http.get<PlatoResumen[]>(`${this.api}/platos`);
  }

  listarZonas(): Observable<ZonaResumen[]> {
    return this.http.get<ZonaResumen[]>(`${this.api}/zonas`);
  }

  obtenerMenus(): Observable<PageResponse<unknown>> {
    return this.http.get<PageResponse<unknown>>(`${this.api}/menus/paginado?page=0&size=1`);
  }

  obtenerClientes(): Observable<PageResponse<unknown>> {
    return this.http.get<PageResponse<unknown>>(`${this.api}/clientes/paginado?page=0&size=1`);
  }

  obtenerPedidos(): Observable<PageResponse<PedidoResumen>> {
    return this.http.get<PageResponse<PedidoResumen>>(`${this.api}/pedidos?page=0&size=5`);
  }
}
