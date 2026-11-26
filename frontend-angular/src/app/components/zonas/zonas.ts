import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Zona } from '../../models/zona';
import { ZonaService } from '../../services/zona.service';

interface ZonaForm {
  idZona?: number;
  nombre: string;
  tarifaDelivery: number | null;
}

@Component({
  selector: 'app-zonas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './zonas.html',
  styleUrl: './zonas.css'
})
export class Zonas implements OnInit {
  zonas: Zona[] =  [];
  texto = '';
  pagina = 0;
  tamano = 5;
  totalElementos = 0;
  totalPaginas = 0;
  cargando = false;
  error = '';

  modalAbierto = false;
  guardando = false;
  esEdicion = false;
  zonaForm: ZonaForm = this.nuevaZona();

  constructor(private zonaService: ZonaService) {}

  ngOnInit(): void {
    this.cargar(0);
  }

  cargar(pagina: number): void {
    this.cargando = true;
    this.error = '';
    this.zonaService.listarPaginado(pagina, this.tamano, this.texto).subscribe({
      next: (resultado) => {
        this.zonas = resultado.content;
        this.pagina = resultado.number;
        this.totalElementos = resultado.totalElements;
        this.totalPaginas = resultado.totalPages;
        this.cargando = false;
      },
      error: (err) => {
        this.error = this.mensajeError(err, 'No se pudieron cargar las zonas.');
        this.cargando = false;
      }
    });
  }

  buscar(): void {
    this.cargar(0);
  }

  limpiar(): void {
    this.texto = '';
    this.cargar(0);
  }

  abrirNueva(): void {
    this.esEdicion = false;
    this.zonaForm = this.nuevaZona();
    this.modalAbierto = true;
  }

  abrirEditar(zona: Zona): void {
    this.esEdicion = true;
    this.zonaForm = {
      idZona: zona.idZona,
      nombre: zona.nombre ?? '',
      tarifaDelivery: zona.tarifaDelivery ?? 0
    };
    this.modalAbierto = true;
  }

  cerrarModal(): void {
    this.modalAbierto = false;
  }

  guardar(): void {
    if (!this.zonaForm.nombre.trim()) {
      this.aviso('El nombre es obligatorio', 'warning');
      return;
    }
    if (this.zonaForm.tarifaDelivery === null || this.zonaForm.tarifaDelivery < 0) {
      this.aviso('La tarifa no puede ser negativa', 'warning');
      return;
    }

    this.guardando = true;
    const payload: Zona = {
      nombre: this.zonaForm.nombre.trim(),
      tarifaDelivery: this.zonaForm.tarifaDelivery
    };
    const peticion = this.esEdicion && this.zonaForm.idZona
      ? this.zonaService.actualizar(this.zonaForm.idZona, payload)
      : this.zonaService.crear(payload);
    peticion.subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarModal();
        this.aviso(this.esEdicion ? 'Zona actualizada' : 'Zona creada', 'success');
        this.cargar(this.pagina);
      },
      error: (err) => {
        this.guardando = false;
        this.aviso(this.mensajeError(err, 'No se pudo guardar la zona.'), 'error');
      }
    });
  }

  cambiarEstado(zona: Zona): void {
    const activar = !zona.activo;
    const Swal = (window as any).Swal;
    const confirmar = Swal
      ? Swal.fire({
          title: activar ? '¿Activar zona?' : '¿Desactivar zona?',
          text: activar
            ? `La zona "${zona.nombre}" volverá a estar disponible.`
            : `La zona "${zona.nombre}" dejará de estar disponible.`,
          icon: 'question',
          showCancelButton: true,
          confirmButtonText: activar ? 'Activar' : 'Desactivar',
          cancelButtonText: 'Cancelar'
        })
      : Promise.resolve({ isConfirmed: true });
    confirmar.then((respuesta: any) => {
      if (!respuesta.isConfirmed || !zona.idZona) return;
      const peticion = activar
        ? this.zonaService.activar(zona.idZona)
        : this.zonaService.desactivar(zona.idZona);
      peticion.subscribe({
        next: () => {
          this.aviso(activar ? 'Zona activada' : 'Zona desactivada', 'success');
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

  private nuevaZona(): ZonaForm {
    return { nombre: '', tarifaDelivery: 0 };
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
