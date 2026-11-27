import { Zona } from './zona';

export interface ClienteUsuario {
  idUsuario?: number;
  nombres?: string;
  apellidos?: string;
  login?: string;
  clave?: string;
  email?: string;
  activo?: boolean;
}

export interface Cliente {
  idCliente?: number;
  usuario?: ClienteUsuario;
  telefono?: string;
  direccion?: string;
  referencia?: string;
  zona?: Zona;
  activo?: boolean;
}
