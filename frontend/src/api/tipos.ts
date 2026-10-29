// Contratos de la API (ver backend/.../web/Dtos.java). Fechas en hora local de Lima, sin zona.

export type Rol = 'ADMIN' | 'MOZO' | 'COCINA' | 'CAJA'
export type TipoPedido = 'MESA' | 'LLEVAR' | 'DELIVERY'
export type EstadoPedido =
  | 'ENVIADO'
  | 'CONFIRMADO'
  | 'EN_PREPARACION'
  | 'LISTO'
  | 'SERVIDO'
  | 'EN_CAMINO'
  | 'ENTREGADO'
  | 'ANULADO'
export type EstadoItem = 'PENDIENTE' | 'EN_PREPARACION' | 'LISTO' | 'SERVIDO' | 'ANULADO'
export type EstadoCuenta = 'ABIERTA' | 'CUENTA' | 'PAGADA'
export type Medio = 'EFECTIVO' | 'YAPE' | 'PLIN' | 'TARJETA'

export interface Estacion {
  id: number
  codigo: string
  nombre: string
  color: string
}

export interface Categoria {
  id: number
  nombre: string
}

export interface Plato {
  id: number
  categoriaId: number
  estacionId: number
  nombre: string
  descripcion: string | null
  precio: number
  foto: string | null
  disponible: boolean
  activo: boolean
}

export interface Carta {
  estaciones: Estacion[]
  categorias: Categoria[]
  platos: Plato[]
}

export interface Local {
  nombre: string
  tipo: string
  direccion: string
  distrito: string
  horario: string
  celular: string
  yapeDemo: string
  demo: boolean
  ahora: string
}

export interface Item {
  id: number
  platoId: number
  estacionId: number
  nombre: string
  cantidad: number
  precioUnit: number
  nota: string | null
  estado: EstadoItem
  actualizadoEn: string
}

export interface Pedido {
  id: number
  codigo: string
  numero: number
  tipo: TipoPedido
  estado: EstadoPedido
  mesa: number | null
  atencionId: number | null
  clienteNombre: string | null
  clienteCelular: string | null
  direccion: string | null
  referencia: string | null
  nota: string | null
  total: number
  pagado: boolean
  creadoEn: string
  actualizadoEn: string
  items: Item[]
}

export interface MesaPublica {
  numero: number
  zona: string
  estadoCuenta: EstadoCuenta | null
  total: number
  pedidos: Pedido[]
}

export interface Atencion {
  id: number
  estado: EstadoCuenta
  abiertaEn: string
  cuentaEn: string | null
  consumo: number
  porConfirmar: number
  listos: number
}

export interface Mesa {
  id: number
  numero: number
  zona: string
  capacidad: number
  token: string
  atencion: Atencion | null
}

export interface Cuenta {
  atencionId: number
  mesa: number
  estado: EstadoCuenta
  pedidos: Pedido[]
  total: number
  pagado: number
  saldo: number
}

export interface Pago {
  id: number
  medio: Medio
  monto: number
  recibido: number | null
  vuelto: number | null
  cobradoPor: string
  creadoEn: string
}

export interface Cierre {
  fecha: string
  totalEfectivo: number
  totalYape: number
  totalPlin: number
  totalTarjeta: number
  total: number
  efectivoContado: number
  diferencia: number
  tickets: number
  cerradoPor: string
  cerradoEn: string
}

export interface Resumen {
  fecha: string
  porMedio: Record<Medio, number>
  total: number
  tickets: number
  ticketPromedio: number
  ventasPorHora: number[]
  masVendidos: { nombre: string; cantidad: number; importe: number }[]
  mesasAbiertas: number
  externosSinCobrar: number
  cierre: Cierre | null
}

export interface Usuario {
  email: string
  nombre: string
  rol: Rol
}

export interface LineaCarrito {
  platoId: number
  cantidad: number
  nota: string
}
