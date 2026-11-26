export type EstadoPedido =
  | 'PENDIENTE'
  | 'CONFIRMADO'
  | 'EN_PREPARACION'
  | 'EN_RUTA'
  | 'ENTREGADO'
  | 'CANCELADO';

export interface UsuarioPedido {
  idUsuario?: number;
  nombres?: string;
  apellidos?: string;
  login?: string;
  email?: string;
}

export interface ZonaPedido {
  idZona: number;
  nombre?: string;
  tarifaDelivery?: number;
  activo?: boolean;
}

export interface ClientePedido {
  idCliente: number;
  usuario?: UsuarioPedido;
  telefono?: string;
  direccion?: string;
  referencia?: string;
  activo?: boolean;
  zona?: ZonaPedido;
}

export interface PlatoPedido {
  idPlato: number;
  nombre?: string;
  precio?: number;
}

export interface DetallePedido {
  idDetalle?: number;
  fechaConsumo?: string;
  diaSemana?: string;
  plato?: PlatoPedido;
  cantidad?: number;
  precioUnitario?: number;
  subtotal?: number;
}

export interface MenuPedido {
  idMenu?: number;
  fechaInicio?: string;
  fechaFin?: string;
  estado?: string;
}

export interface Pedido {
  idPedido: number;
  cliente?: ClientePedido;
  menuSemanal?: MenuPedido;
  fechaCreacion?: string;
  estado?: EstadoPedido;
  total?: number;
  direccionEntrega?: string;
  zona?: ZonaPedido;
  tarifaDelivery?: number;
  horarioEntrega?: string;
  metodoPago?: string;
  observaciones?: string | null;
  detalles?: DetallePedido[];
}

export interface PedidoFiltros {
  estado: EstadoPedido | '';
  idCliente: number | null;
  idZona: number | null;
  fechaDesde: string;
  fechaHasta: string;
}
