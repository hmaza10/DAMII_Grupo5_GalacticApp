import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ClientePedido,
  EstadoPedido,
  Pedido,
  PedidoFiltros,
  ZonaPedido
} from '../models/pedido';
import { PageResponse } from '../models/paginacion';

@Injectable({ providedIn: 'root' })
export class PedidoService {
  private readonly api = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  listar(filtros: PedidoFiltros, page: number, size: number): Observable<PageResponse<Pedido>> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));

    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    if (filtros.idCliente !== null) {
      params = params.set('idCliente', String(filtros.idCliente));
    }
    if (filtros.idZona !== null) {
      params = params.set('idZona', String(filtros.idZona));
    }
    if (filtros.fechaDesde) {
      params = params.set('fechaDesde', filtros.fechaDesde);
    }
    if (filtros.fechaHasta) {
      params = params.set('fechaHasta', filtros.fechaHasta);
    }

    return this.http.get<PageResponse<Pedido>>(`${this.api}/pedidos`, { params });
  }

  obtener(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.api}/pedidos/${id}`);
  }

  cambiarEstado(id: number, estado: EstadoPedido): Observable<Pedido> {
    const params = new HttpParams().set('estado', estado);
    return this.http.patch<Pedido>(`${this.api}/pedidos/${id}/estado`, null, { params });
  }

  descargarPdf(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.api}/pedidos/${id}/pdf`, {
      responseType: 'blob',
      observe: 'response'
    });
  }

  listarZonas(): Observable<ZonaPedido[]> {
    return this.http.get<ZonaPedido[]>(`${this.api}/zonas`);
  }

  listarClientes(): Observable<ClientePedido[]> {
    return this.http.get<ClientePedido[]>(`${this.api}/clientes`);
  }

  buscarClientes(texto: string): Observable<ClientePedido[]> {
    const params = new HttpParams().set('texto', texto);
    return this.http.get<ClientePedido[]>(`${this.api}/clientes/buscarPorNombre`, { params });
  }
}
