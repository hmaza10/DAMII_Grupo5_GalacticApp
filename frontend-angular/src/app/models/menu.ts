export type EstadoMenu = 'BORRADOR' | 'PUBLICADO';

export interface PlatoReferencia {
  idPlato: number;
  nombre?: string;
  precio?: number;
  activo?: boolean;
}

export interface PlatoSeleccionable {
  idPlato: number;
  nombre: string;
  precio: number;
  activo: boolean;
  descripcion?: string;
  imagenUrl?: string;
  categoria?: { idCategoria: number; nombre: string };
}

export interface MenuDia {
  idMenuDia?: number;
  fecha?: string;
  diaSemana?: string;
  opcion1?: PlatoReferencia | null;
  opcion2?: PlatoReferencia | null;
}

export interface MenuSemanal {
  idMenu?: number;
  fechaInicio: string;
  fechaFin?: string;
  estado?: EstadoMenu;
  publicadoEn?: string | null;
  dias: MenuDia[];
}

export interface DiaFormulario {
  opcion1: number | null;
  opcion2: number | null;
}
