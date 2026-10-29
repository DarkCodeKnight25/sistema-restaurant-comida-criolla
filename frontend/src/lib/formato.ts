import type { EstadoPedido, Medio, TipoPedido } from '../api/tipos'

export function soles(monto: number): string {
  return 'S/ ' + monto.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** Precio de cartel: sin decimales si es entero («S/ 38»), como se escribe a plumón. */
export function precioCartel(monto: number): string {
  return Number.isInteger(monto) ? `S/ ${monto}` : soles(monto)
}

export function numeroPedido(n: number): string {
  return '#' + String(n).padStart(3, '0')
}

export const ESTADO_PEDIDO: Record<EstadoPedido, string> = {
  ENVIADO: 'Enviado, por confirmar',
  CONFIRMADO: 'Confirmado, en cola',
  EN_PREPARACION: 'En preparación',
  LISTO: 'Listo',
  SERVIDO: 'Servido',
  EN_CAMINO: 'En camino',
  ENTREGADO: 'Entregado',
  ANULADO: 'Anulado',
}

export const TIPO: Record<TipoPedido, string> = {
  MESA: 'Mesa',
  LLEVAR: 'Para llevar',
  DELIVERY: 'Delivery',
}

export const MEDIO: Record<Medio, string> = {
  EFECTIVO: 'Efectivo',
  YAPE: 'Yape',
  PLIN: 'Plin',
  TARJETA: 'Tarjeta',
}

export function plural(n: number, uno: string, varios: string) {
  return `${n} ${n === 1 ? uno : varios}`
}
