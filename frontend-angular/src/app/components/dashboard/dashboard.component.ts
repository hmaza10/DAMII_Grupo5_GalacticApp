import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { DashboardService, PedidoResumen } from '../../services/dashboard.service';
import { SesionService } from '../../services/sesion.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  cargando = true;
  errorMensaje = '';
  nombreUsuario = '';

  totalCategorias = 0;
  totalPlatos = 0;
  totalMenus = 0;
  totalClientes = 0;
  totalPedidos = 0;
  totalZonas = 0;

  ultimosPedidos: PedidoResumen[] = [];

  constructor(
    private dashboardService: DashboardService,
    private sesionService: SesionService,
  ) {}

  ngOnInit(): void {
    const usuario = this.sesionService.obtenerUsuario();
    this.nombreUsuario = usuario ? `${usuario.nombres} ${usuario.apellidos}` : '';

    forkJoin({
      categorias: this.dashboardService.listarCategorias(),
      platos: this.dashboardService.listarPlatos(),
      zonas: this.dashboardService.listarZonas(),
      menus: this.dashboardService.obtenerMenus(),
      clientes: this.dashboardService.obtenerClientes(),
      pedidos: this.dashboardService.obtenerPedidos(),
    }).subscribe({
      next: (data) => {
        this.totalCategorias = data.categorias.length;
        this.totalPlatos = data.platos.length;
        this.totalZonas = data.zonas.length;
        this.totalMenus = data.menus.totalElements;
        this.totalClientes = data.clientes.totalElements;
        this.totalPedidos = data.pedidos.totalElements;
        this.ultimosPedidos = data.pedidos.content.slice(0, 5);
        this.cargando = false;
      },
      error: (error) => {
        console.error('Error al cargar el dashboard', error);
        this.errorMensaje = 'No se pudieron cargar los datos del dashboard.';
        this.cargando = false;
      },
    });
  }

  nombreCliente(pedido: PedidoResumen): string {
    const usuario = pedido.cliente?.usuario;
    const nombre = [usuario?.nombres, usuario?.apellidos].filter(Boolean).join(' ');

    return nombre || 'Cliente';
  }
}
