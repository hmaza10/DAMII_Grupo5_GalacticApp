import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Cliente } from '../../models/cliente';
import { Zona } from '../../models/zona';
import { ClienteService } from '../../services/cliente.service';
import { ZonaService } from '../../services/zona.service';

interface ClienteForm {
  idCliente?: number;
  nombres: string;
  apellidos: string;
  login: string;
  clave: string;
  email: string;
  telefono: string;
  direccion: string;
  referencia: string;
  idZona: number | null;
}

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clientes.html',
  styleUrl: './clientes.css'
})
export class Clientes implements OnInit {
  clientes: Cliente[] = [];
  zonas: Zona[] = [];
  texto = '';
  criterio = 'nombre';
  idZona: number | null = null;
  pagina = 0;
  tamano = 5;
  totalElementos = 0;
  totalPaginas = 0;
  cargando = false;
  error = '';

  modalAbierto = false;
  guardando = false;
  esEdicion = false;
  clienteForm: ClienteForm = this.nuevoCliente();

  constructor(
    private clienteService: ClienteService,
    private zonaService: ZonaService
  ) {}

  ngOnInit(): void {
    this.cargarZonas();
    this.cargar(0);
  }

  cargarZonas(): void {
    this.zonaService.listarPaginado(0, 50, '').subscribe({
      next: (resultado) => this.zonas = resultado.content.filter((zona) => zona.activo),
      error: () => this.zonas = []
    });
  }

  cargar(pagina: number): void {
    this.cargando = true;
    this.error = '';
    this.clienteService
      .listarPaginado(pagina, this.tamano, this.criterio, this.texto, this.idZona)
      .subscribe({
        next: (resultado) => {
          this.clientes = resultado.content;
          this.pagina = resultado.number;
          this.totalElementos = resultado.totalElements;
          this.totalPaginas = resultado.totalPages;
          this.cargando = false;
        },
        error: (err) => {
          this.error = this.mensajeError(err, 'No se pudieron cargar los clientes.');
          this.cargando = false;
        }
      });
  }

  buscar(): void {
    this.cargar(0);
  }

  limpiar(): void {
    this.texto = '';
    this.criterio = 'nombre';
    this.idZona = null;
    this.cargar(0);
  }

  nombreCompleto(cliente: Cliente): string {
    const nombres = cliente.usuario?.nombres ?? '';
    const apellidos = cliente.usuario?.apellidos ?? '';
    return `${nombres} ${apellidos}`.trim() || '-';
  }

  abrirNuevo(): void {
    this.esEdicion = false;
    this.clienteForm = this.nuevoCliente();
    this.modalAbierto = true;
  }

  abrirEditar(cliente: Cliente): void {
    this.esEdicion = true;
    this.clienteForm = {
      idCliente: cliente.idCliente,
      nombres: cliente.usuario?.nombres ?? '',
      apellidos: cliente.usuario?.apellidos ?? '',
      login: cliente.usuario?.login ?? '',
      clave: '',
      email: cliente.usuario?.email ?? '',
      telefono: cliente.telefono ?? '',
      direccion: cliente.direccion ?? '',
      referencia: cliente.referencia ?? '',
      idZona: cliente.zona?.idZona ?? null
    };
    this.modalAbierto = true;
  }

  cerrarModal(): void {
    this.modalAbierto = false;
  }

  guardar(): void {
    const error = this.validar();
    if (error) {
      this.aviso(error, 'warning');
      return;
    }

    this.guardando = true;
    const payload: Cliente = {
      telefono: this.clienteForm.telefono.trim(),
      direccion: this.clienteForm.direccion.trim(),
      referencia: this.clienteForm.referencia || undefined,
      zona: { idZona: this.clienteForm.idZona! },
      usuario: this.esEdicion
        ? {
            nombres: this.clienteForm.nombres.trim(),
            apellidos: this.clienteForm.apellidos.trim(),
            email: this.clienteForm.email.trim()
          }
        : {
            nombres: this.clienteForm.nombres.trim(),
            apellidos: this.clienteForm.apellidos.trim(),
            login: this.clienteForm.login.trim(),
            clave: this.clienteForm.clave,
            email: this.clienteForm.email.trim()
          }
    };
    const peticion = this.esEdicion && this.clienteForm.idCliente
      ? this.clienteService.actualizar(this.clienteForm.idCliente, payload)
      : this.clienteService.crear(payload);
    peticion.subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarModal();
        this.aviso(this.esEdicion ? 'Cliente actualizado' : 'Cliente creado', 'success');
        this.cargar(this.pagina);
      },
      error: (err) => {
        this.guardando = false;
        this.aviso(this.mensajeError(err, 'No se pudo guardar el cliente.'), 'error');
      }
    });
  }

  cambiarEstado(cliente: Cliente): void {
    const activar = !cliente.activo;
    const Swal = (window as any).Swal;
    const confirmar = Swal
      ? Swal.fire({
          title: activar ? '¿Activar cliente?' : '¿Desactivar cliente?',
          text: activar
            ? `El cliente "${this.nombreCompleto(cliente)}" volverá a estar activo.`
            : `El cliente "${this.nombreCompleto(cliente)}" dejará de estar activo.`,
          icon: 'question',
          showCancelButton: true,
          confirmButtonText: activar ? 'Activar' : 'Desactivar',
          cancelButtonText: 'Cancelar'
        })
      : Promise.resolve({ isConfirmed: true });
    confirmar.then((respuesta: any) => {
      if (!respuesta.isConfirmed || !cliente.idCliente) return;
      const peticion = activar
        ? this.clienteService.activar(cliente.idCliente)
        : this.clienteService.desactivar(cliente.idCliente);
      peticion.subscribe({
        next: () => {
          this.aviso(activar ? 'Cliente activado' : 'Cliente desactivado', 'success');
          this.cargar(this.pagina);
        },
        error: (err) => this.aviso(this.mensajeError(err, 'No se pudo cambiar el estado.'), 'error')
      });
    });
  }

  irPagina(delta: number): void {
    const destino = this.pagina + delta;
    if (destino < 0 || destino >= this.totalPaginas) return;
    this.cargar(destino);
  }

  private nuevoCliente(): ClienteForm {
    return {
      nombres: '',
      apellidos: '',
      login: '',
      clave: '',
      email: '',
      telefono: '',
      direccion: '',
      referencia: '',
      idZona: null
    };
  }

  private validar(): string {
    if (!this.clienteForm.nombres.trim() || !this.clienteForm.apellidos.trim()) {
      return 'Los nombres y apellidos son obligatorios';
    }
    if (!this.esEdicion && !/^[A-Za-z0-9._-]{4,30}$/.test(this.clienteForm.login.trim())) {
      return 'El usuario debe tener de 4 a 30 caracteres (letras, números, punto, guion o guion bajo)';
    }
    if (!this.esEdicion && !this.clienteForm.clave) {
      return 'La clave es obligatoria';
    }
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(this.clienteForm.email.trim())) {
      return 'Ingrese un correo válido';
    }
    if (!/^\d{9}$/.test(this.clienteForm.telefono.trim())) {
      return 'El teléfono debe tener 9 dígitos';
    }
    if (!this.clienteForm.direccion.trim()) {
      return 'La dirección es obligatoria';
    }
    if (!this.clienteForm.idZona) {
      return 'Debe seleccionar una zona';
    }
    return '';
  }

  private aviso(mensaje: string, icono: string): void {
    const Swal = (window as any).Swal;
    if (Swal) {
      Swal.fire({ title: mensaje, icon: icono, timer: 2200, showConfirmButton: false });
    } else if (icono === 'error') {
      this.error = mensaje;
    }
  }

  private mensajeError(err: any, porDefecto: string): string {
    const cuerpo = err?.error;
    if (typeof cuerpo === 'string' && cuerpo.trim()) return cuerpo;
    if (cuerpo && typeof cuerpo === 'object') {
      if (typeof cuerpo.mensaje === 'string') return cuerpo.mensaje;
      const valores = Object.values(cuerpo).filter((valor): valor is string => typeof valor === 'string');
      if (valores.length) return valores.join(' · ');
    }
    return porDefecto;
  }
}
