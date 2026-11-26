import { Categoria } from './categoria';

export interface Plato {
  idPlato?: number;
  categoria?: Categoria;
  nombre: string;
  descripcion?: string;
  precio?: number;
  imagenUrl?: string;
  activo?: boolean;
}
