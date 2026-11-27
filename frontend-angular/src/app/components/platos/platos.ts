import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Categoria } from '../../models/categoria';
import { Plato } from '../../models/plato';
import { CategoriaService } from '../../services/categoria.service';
import { PlatoService } from '../../services/plato.service';

interface PlatoForm {
  idPlato?: number;
  nombre: string;
  descripcion: string;
  precio: number | null;
  imagenUrl: string;
  idCategoria: number | null;
}

@Component({
  selector: 'app-platos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './platos.html',
  styleUrl: './platos.css'
})
export class Platos implements OnInit {
  platos: Plato[] = [];
  categorias: Categoria[] = [];
  texto = '';
  criterio = 'nombre';
  idCategoria: number | null = null;
  pagina = 0;
  tamano = 5;
  totalElementos = 0;
  totalPaginas = 0;
  cargando = false;
  error = '';

  modalAbierto = false;
  guardando = false;
  esEdicion = false;
  platoForm: PlatoForm = this.nuevoPlato();

  constructor(
    private platoService: PlatoService,
    private categoriaService: CategoriaService
  ) {}

  ngOnInit(): void {
    this.cargarCategorias();
    this.cargar(0);
  }

  cargarCategorias(): void {
    this.categoriaService.listarPaginado(0, 50, '').subscribe({
      next: (resultado) => this.categorias = resultado.content.filter((categoria) => categoria.activo),
      error: () => this.categorias = []
    });
  }

  cargar(pagina: number): void {
    this.cargando = true;
    this.error = '';
    this.platoService
      .listarPaginado(pagina, this.tamano, this.criterio, this.texto, this.idCategoria)
      .subscribe({
        next: (resultado) => {
          this.platos = resultado.content;
          this.pagina = resultado.number;
          this.totalElementos = resultado.totalElements;
          this.totalPaginas = resultado.totalPages;
          this.cargando = false;
        },
        error: (err) => {
          this.error = this.mensajeError(err, 'No se pudieron cargar los platos.');
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
    this.idCategoria = null;
    this.cargar(0);
  }

  abrirNuevo(): void {
    this.esEdicion = false;
    this.platoForm = this.nuevoPlato();
    this.modalAbierto = true;
  }

  abrirEditar(plato: Plato): void {
    this.esEdicion = true;
    this.platoForm = {
      idPlato: plato.idPlato,
      nombre: plato.nombre,
      descripcion: plato.descripcion ?? '',
      precio: plato.precio ?? null,
      imagenUrl: plato.imagenUrl ?? '',
      idCategoria: plato.categoria?.idCategoria ?? null
    };
    this.modalAbierto = true;
  }

  cerrarModal(): void {
    this.modalAbierto = false;
  }

  guardar(): void {
    if (!this.platoForm.nombre.trim()) {
      this.aviso('El nombre es obligatorio', 'warning');
      return;
    }
    const nombre = this.platoForm.nombre.trim();
    if (this.nombreDuplicado(nombre)) {
      this.aviso('Ya existe un plato activo con ese nombre', 'warning');
      return;
    }
    if (!this.platoForm.precio || this.platoForm.precio <= 0) {
      this.aviso('El precio debe ser mayor a 0', 'warning');
      return;
    }
    if (!this.platoForm.idCategoria) {
      this.aviso('Debe seleccionar una categoría', 'warning');
      return;
    }

    this.guardando = true;
    const payload: Plato = {
      nombre,
      descripcion: this.platoForm.descripcion || undefined,
      precio: this.platoForm.precio,
      imagenUrl: this.platoForm.imagenUrl || undefined,
      categoria: { idCategoria: this.platoForm.idCategoria }
    };
    const peticion = this.esEdicion && this.platoForm.idPlato
      ? this.platoService.actualizar(this.platoForm.idPlato, payload)
      : this.platoService.crear(payload);
    peticion.subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarModal();
        this.aviso(this.esEdicion ? 'Plato actualizado' : 'Plato creado', 'success');
        this.cargar(this.pagina);
      },
      error: (err) => {
        this.guardando = false;
        this.aviso(this.mensajeError(err, 'No se pudo guardar el plato.'), 'error');
      }
    });
  }

  cambiarEstado(plato: Plato): void {
    const activar = !plato.activo;
    const Swal = (window as any).Swal;
    const confirmar = Swal
      ? Swal.fire({
          title: activar ? '¿Activar plato?' : '¿Desactivar plato?',
          text: activar
            ? `El plato "${plato.nombre}" volverá a estar disponible.`
            : `El plato "${plato.nombre}" dejará de estar disponible.`,
          icon: 'question',
          showCancelButton: true,
          confirmButtonText: activar ? 'Activar' : 'Desactivar',
          cancelButtonText: 'Cancelar'
        })
      : Promise.resolve({ isConfirmed: true });
    confirmar.then((respuesta: any) => {
      if (!respuesta.isConfirmed || !plato.idPlato) return;
      const peticion = activar
        ? this.platoService.activar(plato.idPlato)
        : this.platoService.desactivar(plato.idPlato);
      peticion.subscribe({
        next: () => {
          this.aviso(activar ? 'Plato activado' : 'Plato desactivado', 'success');
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

  private nuevoPlato(): PlatoForm {
    return {
      nombre: '',
      descripcion: '',
      precio: null,
      imagenUrl: '',
      idCategoria: null
    };
  }

  private nombreDuplicado(nombre: string): boolean {
    const idActual = this.esEdicion ? this.platoForm.idPlato : undefined;
    return this.platos.some((plato) => plato.activo
      && plato.nombre.trim().toLowerCase() === nombre.toLowerCase()
      && plato.idPlato !== idActual);
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
