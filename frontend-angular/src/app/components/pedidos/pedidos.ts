import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import {
  ClientePedido,
  EstadoPedido,
  Pedido,
  PedidoFiltros,
  ZonaPedido
} from '../../models/pedido';
import { PageResponse } from '../../models/paginacion';
import { mensajeErrorApi } from '../../services/api-error';
import { confirmarAccion } from '../../services/confirmacion';
import { PedidoService } from '../../services/pedido.service';

const ESTADOS: EstadoPedido[] = [
  'PENDIENTE',
  'CONFIRMADO',
  'EN_PREPARACION',
  'EN_RUTA',
  'ENTREGADO',
  'CANCELADO',
];

const TRANSICIONES: Record<EstadoPedido, EstadoPedido[]> = {
  PENDIENTE: ['CONFIRMADO', 'CANCELADO'],
  CONFIRMADO: ['EN_PREPARACION', 'CANCELADO'],
  EN_PREPARACION: ['EN_RUTA'],
  EN_RUTA: ['ENTREGADO'],
  ENTREGADO: [],
  CANCELADO: [],
};

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedidos.html',
  styleUrl: './pedidos.css',
})
export class Pedidos implements OnInit, OnDestroy {
  private readonly suscripciones = new Subscription();
  private busquedaSuscripcion: Subscription | null = null;

  readonly estados = ESTADOS;

  pedidos: Pedido[] = [];
  clientes: ClientePedido[] = [];
  todosClientes: ClientePedido[] = [];
  zonas: ZonaPedido[] = [];

  filtros: PedidoFiltros = {
    estado: '',
    idCliente: null,
    idZona: null,
    fechaDesde: '',
    fechaHasta: '',
  };
  busquedaCliente = '';

  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;

  cargando = false;
  errorLista = '';
  errorAux = '';
  mensajeExito = '';
  mensajeError = '';

  detalle: Pedido | null = null;
  cargandoDetalle = false;
  errorDetalle = '';
  procesandoEstadoId: number | null = null;
  descargandoId: number | null = null;

  constructor(private pedidoService: PedidoService) {}

  ngOnInit(): void {
    this.cargarPedidos();
    this.cargarAuxiliares();
  }

  ngOnDestroy(): void {
    this.suscripciones.unsubscribe();
    this.busquedaSuscripcion?.unsubscribe();
  }

  cargarPedidos(): void {
    this.cargando = true;
    this.errorLista = '';

    this.suscripciones.add(
      this.pedidoService.listar(this.filtros, this.page, this.size).subscribe({
        next: (respuesta: PageResponse<Pedido>) => {
          this.pedidos = respuesta.content ?? [];
          this.totalElements = respuesta.totalElements ?? 0;
          this.totalPages = respuesta.totalPages ?? 0;
          this.page = respuesta.number ?? this.page;
          this.cargando = false;
        },
        error: (error: unknown) => {
          this.pedidos = [];
          this.totalElements = 0;
          this.totalPages = 0;
          this.errorLista = mensajeErrorApi(error, 'No se pudieron cargar los pedidos.');
          this.cargando = false;
        },
      }),
    );
  }

  aplicarFiltros(): void {
    if (
      this.filtros.fechaDesde &&
      this.filtros.fechaHasta &&
      this.filtros.fechaDesde > this.filtros.fechaHasta
    ) {
      this.mensajeError = 'La fecha final no puede ser anterior a la inicial.';
      return;
    }

    this.mensajeError = '';
    this.page = 0;
    this.cargarPedidos();
  }

  limpiarFiltros(): void {
    this.filtros = {
      estado: '',
      idCliente: null,
      idZona: null,
      fechaDesde: '',
      fechaHasta: '',
    };
    this.busquedaCliente = '';
    this.clientes = this.todosClientes;
    this.aplicarFiltros();
  }

  irPagina(pagina: number): void {
    if (pagina < 0 || pagina >= this.totalPages || pagina === this.page) {
      return;
    }
    this.page = pagina;
    this.cargarPedidos();
  }

  buscarCliente(): void {
    const texto = this.busquedaCliente.trim();

    if (!texto) {
      this.clientes = this.todosClientes;
      return;
    }

    this.busquedaSuscripcion?.unsubscribe();
    this.busquedaSuscripcion = this.pedidoService.buscarClientes(texto).subscribe({
      next: (clientes) => {
        this.clientes = clientes;
      },
      error: (error: unknown) => {
        this.errorAux = mensajeErrorApi(error, 'No se pudo buscar clientes.');
      },
    });
  }

  verDetalle(pedido: Pedido): void {
    this.errorDetalle = '';
    this.cargandoDetalle = true;
    this.detalle = null;

    this.suscripciones.add(
      this.pedidoService.obtener(pedido.idPedido).subscribe({
        next: (obtenido) => {
          this.detalle = obtenido;
          this.cargandoDetalle = false;
        },
        error: (error: unknown) => {
          this.errorDetalle = mensajeErrorApi(error, 'No se pudo cargar el detalle del pedido.');
          this.cargandoDetalle = false;
        },
      }),
    );
  }

  cerrarDetalle(): void {
    this.detalle = null;
    this.errorDetalle = '';
  }

  estadosSiguientes(estado: EstadoPedido | undefined): EstadoPedido[] {
    return estado ? TRANSICIONES[estado] : [];
  }

  cambiarEstado(pedido: Pedido, estadoNuevo: EstadoPedido): void {
    const texto = `El pedido #${pedido.idPedido} cambiará de ${pedido.estado ?? 'PENDIENTE'} a ${estadoNuevo}.`;

    confirmarAccion({
      titulo: '¿Cambiar el estado del pedido?',
      texto,
      confirmarTexto: 'Sí, cambiar',
      cancelarTexto: 'Cancelar',
      icono: estadoNuevo === 'CANCELADO' ? 'warning' : 'question',
    }).then((confirmado) => {
      if (!confirmado) {
        return;
      }

      this.procesandoEstadoId = pedido.idPedido;
      this.mensajeError = '';

      this.suscripciones.add(
        this.pedidoService.cambiarEstado(pedido.idPedido, estadoNuevo).subscribe({
          next: (actualizado) => {
            this.procesandoEstadoId = null;
            this.mensajeExito = `Pedido #${actualizado.idPedido} actualizado a ${actualizado.estado ?? estadoNuevo}.`;
            this.pedidos = this.pedidos.map((item) =>
              item.idPedido === actualizado.idPedido ? actualizado : item,
            );
            if (this.detalle?.idPedido === actualizado.idPedido) {
              this.detalle = actualizado;
            }
            this.cargarPedidos();
          },
          error: (error: unknown) => {
            this.procesandoEstadoId = null;
            this.mensajeError = mensajeErrorApi(error, 'No se pudo actualizar el estado del pedido.');
          },
        }),
      );
    });
  }

  descargarPdf(pedido: Pedido): void {
    const id = pedido.idPedido;
    this.descargandoId = id;
    this.mensajeError = '';

    this.suscripciones.add(
      this.pedidoService.descargarPdf(id).subscribe({
        next: (respuesta) => {
          this.descargandoId = null;
          const archivo = respuesta.body;

          if (!archivo || archivo.size === 0) {
            this.mensajeError = 'El servidor devolvió un comprobante vacío.';
            return;
          }

          const tipo = (respuesta.headers.get('content-type') ?? archivo.type ?? '').toLowerCase();
          if (tipo.includes('application/json') || tipo.includes('text/html')) {
            archivo.text().then((texto) => {
              this.mensajeError = this.leerMensaje(texto, 'El servidor no devolvió un PDF válido.');
            });
            return;
          }

          if (tipo.includes('pdf') || archivo.type.toLowerCase().includes('pdf')) {
            this.descargarArchivo(archivo, id);
            return;
          }

          archivo.text().then((texto) => {
            if (texto.startsWith('%PDF-')) {
              this.descargarArchivo(archivo, id);
              return;
            }
            this.mensajeError = this.leerMensaje(texto, 'El servidor no devolvió un PDF válido.');
          });
        },
        error: (error: unknown) => {
          this.descargandoId = null;
          this.mensajeError = mensajeErrorApi(error, 'No se pudo descargar el comprobante.');
        },
      }),
    );
  }

  nombreCliente(pedido: Pedido): string {
    const usuario = pedido.cliente?.usuario;
    const nombre = [usuario?.nombres, usuario?.apellidos].filter(Boolean).join(' ');
    return nombre || 'Cliente';
  }

  nombreClienteLista(cliente: ClientePedido): string {
    const usuario = cliente.usuario;
    const nombre = [usuario?.nombres, usuario?.apellidos].filter(Boolean).join(' ');
    return nombre || `Cliente #${cliente.idCliente}`;
  }

  semana(pedido: Pedido): string {
    const inicio = pedido.menuSemanal?.fechaInicio;
    const fin = pedido.menuSemanal?.fechaFin;
    return inicio && fin ? `${this.formatearFecha(inicio)} - ${this.formatearFecha(fin)}` : '-';
  }

  observaciones(pedido: Pedido): string {
    const texto = pedido.observaciones;
    return texto && texto.trim().length > 0 ? texto : '-';
  }

  subtotalPlatos(pedido: Pedido): number {
    return (pedido.detalles ?? []).reduce((total, detalle) => total + (detalle.subtotal ?? 0), 0);
  }

  claseEstado(estado: EstadoPedido | undefined): string {
    switch (estado) {
      case 'PENDIENTE':
        return 'bg-warning text-dark';
      case 'CONFIRMADO':
        return 'bg-primary';
      case 'EN_PREPARACION':
        return 'bg-info text-dark';
      case 'EN_RUTA':
        return 'bg-secondary';
      case 'ENTREGADO':
        return 'bg-success';
      case 'CANCELADO':
        return 'bg-danger';
      default:
        return 'bg-light text-dark';
    }
  }

  claseTransicion(estado: EstadoPedido): string {
    if (estado === 'CANCELADO') {
      return 'btn-outline-danger';
    }
    if (estado === 'ENTREGADO') {
      return 'btn-success';
    }
    return 'btn-outline-primary';
  }

  private cargarAuxiliares(): void {
    this.suscripciones.add(
      this.pedidoService.listarZonas().subscribe({
        next: (zonas) => {
          this.zonas = zonas;
        },
        error: (error: unknown) => {
          this.errorAux = mensajeErrorApi(error, 'No se pudieron cargar las zonas.');
        },
      }),
    );

    this.suscripciones.add(
      this.pedidoService.listarClientes().subscribe({
        next: (clientes) => {
          this.todosClientes = clientes;
          this.clientes = clientes;
        },
        error: (error: unknown) => {
          this.errorAux = mensajeErrorApi(error, 'No se pudo cargar la lista de clientes.');
        },
      }),
    );
  }

  private descargarArchivo(archivo: Blob, id: number): void {
    const url = URL.createObjectURL(archivo);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `pedido-${id}.pdf`;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    this.mensajeExito = `Comprobante del pedido #${id} descargado.`;
  }

  private leerMensaje(texto: string, alternativa: string): string {
    try {
      const cuerpo: unknown = JSON.parse(texto);
      if (cuerpo !== null && typeof cuerpo === 'object') {
        const registro = cuerpo as Record<string, unknown>;
        const mensaje = registro['mensaje'];
        if (typeof mensaje === 'string' && mensaje.trim().length > 0) {
          return mensaje.trim();
        }
        const valores = Object.values(registro).filter(
          (valor): valor is string => typeof valor === 'string' && valor.trim().length > 0,
        );
        if (valores.length > 0) {
          return valores.join(' · ');
        }
      }
    } catch {
      return alternativa;
    }
    return texto.trim().slice(0, 200) || alternativa;
  }

  private formatearFecha(valor: string): string {
    const partes = valor.slice(0, 10).split('-');
    if (partes.length !== 3) {
      return valor;
    }
    return `${partes[2]}/${partes[1]}/${partes[0]}`;
  }
}
