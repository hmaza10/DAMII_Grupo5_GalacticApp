import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import { DiaFormulario, EstadoMenu, MenuSemanal, PlatoSeleccionable } from '../../models/menu';
import { PageResponse } from '../../models/paginacion';
import { mensajeErrorApi } from '../../services/api-error';
import { confirmarAccion } from '../../services/confirmacion';
import { MenuService } from '../../services/menu.service';

const DIAS = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'];

@Component({
  selector: 'app-menus',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './menus.html',
  styleUrl: './menus.css',
})
export class Menus implements OnInit, OnDestroy {
  private readonly suscripciones = new Subscription();

  menus: MenuSemanal[] = [];
  platos: PlatoSeleccionable[] = [];

  filtroEstado: EstadoMenu | '' = '';
  page = 0;
  size = 5;
  totalElements = 0;
  totalPages = 0;

  cargandoLista = false;
  cargandoPlatos = false;
  cargandoFormulario = false;
  cargandoDetalle = false;
  errorLista = '';
  errorPlatos = '';
  errorFormulario = '';
  errorDetalle = '';
  mensajeExito = '';

  formularioVisible = false;
  modo: 'crear' | 'editar' = 'crear';
  menuId: number | null = null;
  menuEstado: EstadoMenu = 'BORRADOR';
  fechaInicio = '';
  dias: DiaFormulario[] = this.diasVacios();
  hayCambiosSinGuardar = false;
  erroresValidacion: string[] = [];
  guardando = false;
  publicando = false;

  detalle: MenuSemanal | null = null;

  constructor(private menuService: MenuService) {}

  ngOnInit(): void {
    this.cargarPlatos();
    this.cargarMenus();
  }

  ngOnDestroy(): void {
    this.suscripciones.unsubscribe();
  }

  cargarMenus(): void {
    this.cargandoLista = true;
    this.errorLista = '';

    this.suscripciones.add(
      this.menuService.listar(this.page, this.size, this.filtroEstado).subscribe({
        next: (respuesta: PageResponse<MenuSemanal>) => {
          this.menus = respuesta.content ?? [];
          this.totalElements = respuesta.totalElements ?? 0;
          this.totalPages = respuesta.totalPages ?? 0;
          this.page = respuesta.number ?? this.page;
          this.cargandoLista = false;
        },
        error: (error: unknown) => {
          this.menus = [];
          this.totalElements = 0;
          this.totalPages = 0;
          this.errorLista = mensajeErrorApi(error, 'No se pudieron cargar los menús.');
          this.cargandoLista = false;
        },
      }),
    );
  }

  cargarPlatos(): void {
    this.cargandoPlatos = true;
    this.errorPlatos = '';

    this.suscripciones.add(
      this.menuService.listarPlatos().subscribe({
        next: (platos) => {
          this.platos = platos;
          this.cargandoPlatos = false;
        },
        error: (error: unknown) => {
          this.errorPlatos = mensajeErrorApi(error, 'No se pudo cargar la lista de platos.');
          this.cargandoPlatos = false;
        },
      }),
    );
  }

  aplicarFiltro(): void {
    this.page = 0;
    this.cargarMenus();
  }

  irPagina(pagina: number): void {
    if (pagina < 0 || pagina >= this.totalPages || pagina === this.page) {
      return;
    }
    this.page = pagina;
    this.cargarMenus();
  }

  nuevoMenu(): void {
    this.formularioVisible = true;
    this.modo = 'crear';
    this.menuId = null;
    this.menuEstado = 'BORRADOR';
    this.fechaInicio = this.lunesActual();
    this.dias = this.diasVacios();
    this.hayCambiosSinGuardar = false;
    this.erroresValidacion = [];
    this.errorFormulario = '';
    this.mensajeExito = '';
    this.detalle = null;
  }

  editarMenu(menu: MenuSemanal): void {
    if (menu.idMenu === undefined) {
      return;
    }

    const id = menu.idMenu;
    this.errorFormulario = '';
    this.errorDetalle = '';
    this.cargandoFormulario = true;

    this.suscripciones.add(
      this.menuService.obtener(id).subscribe({
        next: (obtenido) => {
          this.modo = 'editar';
          this.menuId = obtenido.idMenu ?? id;
          this.menuEstado = obtenido.estado ?? 'BORRADOR';
          this.fechaInicio = (obtenido.fechaInicio ?? '').slice(0, 10);
          this.dias = DIAS.map(
            (_dia, indice) => ({
              opcion1: obtenido.dias?.[indice]?.opcion1?.idPlato ?? null,
              opcion2: obtenido.dias?.[indice]?.opcion2?.idPlato ?? null,
            }),
          );
          this.formularioVisible = true;
          this.detalle = null;
          this.hayCambiosSinGuardar = false;
          this.erroresValidacion = [];
          this.mensajeExito = '';
          this.cargandoFormulario = false;
        },
        error: (error: unknown) => {
          this.errorFormulario = mensajeErrorApi(error, 'No se pudo cargar el menú.');
          this.cargandoFormulario = false;
        },
      }),
    );
  }

  verDetalle(menu: MenuSemanal): void {
    if (menu.idMenu === undefined) {
      return;
    }

    this.errorDetalle = '';
    this.cargandoDetalle = true;
    this.detalle = null;

    this.suscripciones.add(
      this.menuService.obtener(menu.idMenu).subscribe({
        next: (obtenido) => {
          this.detalle = obtenido;
          this.cargandoDetalle = false;
        },
        error: (error: unknown) => {
          this.errorDetalle = mensajeErrorApi(error, 'No se pudo cargar el detalle del menú.');
          this.cargandoDetalle = false;
        },
      }),
    );
  }

  cerrarDetalle(): void {
    this.detalle = null;
  }

  cerrarFormulario(): void {
    this.formularioVisible = false;
    this.erroresValidacion = [];
    this.errorFormulario = '';
  }

  marcarCambio(): void {
    this.hayCambiosSinGuardar = true;
    this.erroresValidacion = [];
    this.mensajeExito = '';
  }

  guardarBorrador(): void {
    const errores = this.validarFormulario();

    if (errores.length > 0) {
      this.erroresValidacion = errores;
      this.errorFormulario = 'Revise los días incompletos antes de guardar.';
      return;
    }

    this.erroresValidacion = [];
    this.errorFormulario = '';
    this.mensajeExito = '';
    this.guardando = true;

    const menu = this.construirMenu();
    const peticion =
      this.menuId === null
        ? this.menuService.crear(menu)
        : this.menuService.actualizar(this.menuId, menu);

    this.suscripciones.add(
      peticion.subscribe({
        next: (guardado) => {
          this.menuId = guardado.idMenu ?? this.menuId;
          this.menuEstado = guardado.estado ?? 'BORRADOR';
          this.modo = 'editar';
          this.hayCambiosSinGuardar = false;
          this.guardando = false;
          this.mensajeExito = 'Borrador guardado. Ya puede publicarlo cuando esté completo.';
          this.cargarMenus();
        },
        error: (error: unknown) => {
          this.errorFormulario = mensajeErrorApi(error, 'No se pudo guardar el borrador.');
          this.guardando = false;
        },
      }),
    );
  }

  publicarMenu(): void {
    if (this.menuId === null) {
      this.errorFormulario = 'Guarde el borrador antes de publicar el menú.';
      return;
    }

    if (this.hayCambiosSinGuardar) {
      this.errorFormulario = 'Guarde los cambios antes de publicar el menú.';
      return;
    }

    const errores = this.validarFormulario();
    if (errores.length > 0) {
      this.erroresValidacion = errores;
      this.errorFormulario = 'No se puede publicar un menú incompleto.';
      return;
    }

    const id = this.menuId;
    this.errorFormulario = '';

    confirmarAccion({
      titulo: '¿Publicar este menú?',
      texto: 'El menú quedará disponible para que los clientes realicen sus pedidos.',
      confirmarTexto: 'Sí, publicar',
      cancelarTexto: 'Cancelar',
    }).then((confirmado) => {
      if (!confirmado) {
        return;
      }

      this.publicando = true;
      this.suscripciones.add(
        this.menuService.publicar(id).subscribe({
          next: (publicado) => {
            this.menuEstado = publicado.estado ?? 'PUBLICADO';
            this.publicando = false;
            this.mensajeExito = 'El menú fue publicado correctamente.';
            this.cargarMenus();
          },
          error: (error: unknown) => {
            this.errorFormulario = mensajeErrorApi(error, 'No se pudo publicar el menú.');
            this.publicando = false;
          },
        }),
      );
    });
  }

  get puedePublicar(): boolean {
    return this.menuId !== null && !this.hayCambiosSinGuardar && this.menuEstado !== 'PUBLICADO';
  }

  get diasCompletos(): number {
    return this.dias.filter(
      (dia) =>
        dia.opcion1 !== null &&
        dia.opcion2 !== null &&
        dia.opcion1 !== dia.opcion2 &&
        !!this.platoPorId(dia.opcion1) &&
        !!this.platoPorId(dia.opcion2),
    ).length;
  }

  get totalOpcion1(): number {
    return this.dias.reduce(
      (total, dia) => total + (this.platoPorId(dia.opcion1)?.precio ?? 0),
      0,
    );
  }

  etiquetaDia(indice: number): string {
    return DIAS[indice] ?? '';
  }

  platoPorId(id: number | null | undefined): PlatoSeleccionable | undefined {
    if (id === null || id === undefined) {
      return undefined;
    }
    return this.platos.find((plato) => plato.idPlato === id);
  }

  fechaDia(indice: number): Date | null {
    const inicio = this.fechaLocal(this.fechaInicio);
    if (!inicio) {
      return null;
    }
    const fecha = new Date(inicio.getFullYear(), inicio.getMonth(), inicio.getDate() + indice);
    return fecha;
  }

  fechaLocal(valor: string | null | undefined): Date | null {
    if (!valor) {
      return null;
    }
    const partes = valor.slice(0, 10).split('-').map(Number);
    if (partes.length !== 3 || partes.some((parte) => Number.isNaN(parte))) {
      return null;
    }
    return new Date(partes[0], partes[1] - 1, partes[2]);
  }

  diasCompletosDe(menu: MenuSemanal): number {
    return (menu.dias ?? []).filter((dia) => dia?.opcion1?.idPlato && dia?.opcion2?.idPlato).length;
  }

  claseEstado(estado: EstadoMenu | undefined): string {
    return estado === 'PUBLICADO' ? 'bg-success' : 'bg-secondary';
  }

  private validarFormulario(): string[] {
    const errores: string[] = [];

    if (!this.fechaInicio) {
      errores.push('Seleccione la fecha de inicio de la semana.');
    } else if (this.diaDeLaSemana(this.fechaInicio) !== 1) {
      errores.push('La semana debe iniciar un lunes.');
    }

    this.dias.forEach((dia, indice) => {
      const nombre = this.etiquetaDia(indice);

      if (dia.opcion1 === null || dia.opcion2 === null) {
        errores.push(`${nombre}: seleccione dos platos diferentes.`);
        return;
      }
      if (dia.opcion1 === dia.opcion2) {
        errores.push(`${nombre}: las dos opciones deben ser distintas.`);
        return;
      }

      const opcion1 = this.platoPorId(dia.opcion1);
      const opcion2 = this.platoPorId(dia.opcion2);
      if (!opcion1 || !opcion2) {
        errores.push(`${nombre}: elija platos disponibles.`);
      } else if (opcion1.activo === false || opcion2.activo === false) {
        errores.push(`${nombre}: uno de los platos no está disponible.`);
      }
    });

    return errores;
  }

  private construirMenu(): MenuSemanal {
    return {
      fechaInicio: this.fechaInicio,
      dias: this.dias.map((dia) => ({
        opcion1: { idPlato: Number(dia.opcion1) },
        opcion2: { idPlato: Number(dia.opcion2) },
      })),
    };
  }

  private diasVacios(): DiaFormulario[] {
    return DIAS.map(() => ({ opcion1: null, opcion2: null }));
  }

  private lunesActual(): string {
    const hoy = new Date();
    const diaSemana = hoy.getDay();
    const desplazamiento = diaSemana === 0 ? -6 : 1 - diaSemana;
    return this.formatearFecha(new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() + desplazamiento));
  }

  private diaDeLaSemana(fecha: string): number {
    const local = this.fechaLocal(fecha);
    return local ? local.getDay() : -1;
  }

  private formatearFecha(fecha: Date): string {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }
}
