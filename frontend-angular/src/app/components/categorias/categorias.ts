import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Categoria } from '../../models/categoria';
import { CategoriaService } from '../../services/categoria.service';

@Component({
  selector: 'app-categorias',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './categorias.html',
  styleUrl: './categorias.css'
})
export class Categorias implements OnInit {
  categorias: Categoria[] = [];
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
  categoriaForm: Categoria = this.nuevaCategoria();

  constructor(private categoriaService: CategoriaService) {}

  ngOnInit(): void {
    this.cargar(0);
  }

  cargar(pagina: number): void {
    this.cargando = true;
    this.error = '';
    this.categoriaService.listarPaginado(pagina, this.tamano, this.texto).subscribe({
      next: (resultado) => {
        this.categorias = resultado.content;
        this.pagina = resultado.number;
        this.totalElementos = resultado.totalElements;
        this.totalPaginas = resultado.totalPages;
        this.cargando = false;
      },
      error: (err) => {
        this.error = this.mensajeError(err, 'No se pudieron cargar las categorías.');
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
    this.categoriaForm = this.nuevaCategoria();
    this.modalAbierto = true;
  }

  abrirEditar(categoria: Categoria): void {
    this.esEdicion = true;
    this.categoriaForm = { ...categoria };
    this.modalAbierto = true;
  }

  cerrarModal(): void {
    this.modalAbierto = false;
  }

  guardar(): void {
    if (!this.categoriaForm.nombre || !this.categoriaForm.nombre.trim()) {
      this.aviso('El nombre es obligatorio', 'warning');
      return;
    }
    const nombre = this.categoriaForm.nombre.trim();
    if (this.nombreDuplicado(nombre)) {
      this.aviso('Ya existe una categoría activa con ese nombre', 'warning');
      return;
    }
    this.guardando = true;
    const peticion = this.esEdicion && this.categoriaForm.idCategoria
      ? this.categoriaService.actualizar(this.categoriaForm.idCategoria, this.categoriaForm)
      : this.categoriaService.crear(this.categoriaForm);
    peticion.subscribe({
      next: () => {
        this.guardando = false;
        this.cerrarModal();
        this.aviso(this.esEdicion ? 'Categoría actualizada' : 'Categoría creada', 'success');
        this.cargar(this.pagina);
      },
      error: (err) => {
        this.guardando = false;
        this.aviso(this.mensajeError(err, 'No se pudo guardar la categoría.'), 'error');
      }
    });
  }

  cambiarEstado(categoria: Categoria): void {
    const activar = !categoria.activo;
    const Swal = (window as any).Swal;
    const confirmar = Swal
      ? Swal.fire({
          title: activar ? '¿Activar categoría?' : '¿Desactivar categoría?',
          text: activar
            ? `La categoría "${categoria.nombre}" volverá a estar disponible.`
            : `La categoría "${categoria.nombre}" dejará de estar disponible.`,
          icon: 'question',
          showCancelButton: true,
          confirmButtonText: activar ? 'Activar' : 'Desactivar',
          cancelButtonText: 'Cancelar'
        })
      : Promise.resolve({ isConfirmed: true });
    confirmar.then((respuesta: any) => {
      if (!respuesta.isConfirmed) return;
      const id = categoria.idCategoria;
      if (!id) return;
      const peticion = activar
        ? this.categoriaService.activar(id)
        : this.categoriaService.desactivar(id);
      peticion.subscribe({
        next: () => {
          this.aviso(activar ? 'Categoría activada' : 'Categoría desactivada', 'success');
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

  private nuevaCategoria(): Categoria {
    return { nombre: '', descripcion: '' };
  }

  private nombreDuplicado(nombre: string): boolean {
    const idActual = this.esEdicion ? this.categoriaForm.idCategoria : undefined;
    return this.categorias.some((categoria) => categoria.activo
      && categoria.nombre?.trim().toLowerCase() === nombre.toLowerCase()
      && categoria.idCategoria !== idActual);
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
