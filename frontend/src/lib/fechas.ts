// La agenda siempre piensa en hora de Lima, aunque el navegador esté en otra zona.

const ZONA = 'America/Lima'

const partesLima = new Intl.DateTimeFormat('en-CA', {
  timeZone: ZONA,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

/** "Ahora" en Lima como texto local ISO: 2026-10-28T10:05 */
export function ahoraLima(): string {
  const p = Object.fromEntries(partesLima.formatToParts(new Date()).map((x) => [x.type, x.value]))
  return `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}`
}

export function hoyLima(): string {
  return ahoraLima().slice(0, 10)
}

/** Fecha ISO (yyyy-mm-dd) más n días, sin pasar por zonas horarias. */
export function sumarDias(fecha: string, n: number): string {
  const [a, m, d] = fecha.split('-').map(Number)
  const t = new Date(Date.UTC(a, m - 1, d + n))
  return t.toISOString().slice(0, 10)
}

/** 1 = lunes ... 7 = domingo */
export function diaSemana(fecha: string): number {
  const [a, m, d] = fecha.slice(0, 10).split('-').map(Number)
  const dia = new Date(Date.UTC(a, m - 1, d)).getUTCDay()
  return dia === 0 ? 7 : dia
}

export function lunesDe(fecha: string): string {
  return sumarDias(fecha, 1 - diaSemana(fecha))
}

const DIAS_CORTOS = ['lun', 'mar', 'mié', 'jue', 'vie', 'sáb', 'dom']
const DIAS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo']
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'set', 'oct', 'nov', 'dic']
const MESES_LARGOS = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'setiembre', 'octubre', 'noviembre', 'diciembre',
]

export function diaCorto(fecha: string) {
  return DIAS_CORTOS[diaSemana(fecha) - 1]
}

/** "mié 28 oct" */
export function fechaCorta(fecha: string): string {
  const [, m, d] = fecha.slice(0, 10).split('-').map(Number)
  return `${diaCorto(fecha)} ${d} ${MESES[m - 1]}`
}

/** "miércoles 28 de octubre" */
export function fechaLarga(fecha: string): string {
  const [, m, d] = fecha.slice(0, 10).split('-').map(Number)
  return `${DIAS[diaSemana(fecha) - 1]} ${d} de ${MESES_LARGOS[m - 1]}`
}

/** "2026-10-28T10:30:00" -> "10:30" */
export function hora(fechaHora: string): string {
  return fechaHora.slice(11, 16)
}

export function minutosDelDia(fechaHora: string): number {
  const [h, m] = fechaHora.slice(11, 16).split(':').map(Number)
  return h * 60 + m
}

/** "09:30" o "09:30:00" -> 570 */
export function minutosHora(h: string): number {
  const [hh, mm] = h.split(':').map(Number)
  return hh * 60 + mm
}

/** Diferencia en minutos entre dos fechas-hora locales. */
export function minutosEntre(desde: string, hasta: string): number {
  const a = Date.parse(desde.slice(0, 16) + ':00Z')
  const b = Date.parse(hasta.slice(0, 16) + ':00Z')
  return Math.round((b - a) / 60000)
}

/** "en 25 min", "hace 10 min", "en 2 h 05 min" */
export function relativo(desde: string, hasta: string): string {
  const min = minutosEntre(desde, hasta)
  const abs = Math.abs(min)
  const texto = abs < 60 ? `${abs} min` : `${Math.floor(abs / 60)} h ${String(abs % 60).padStart(2, '0')} min`
  if (abs === 0) return 'ahora'
  return min > 0 ? `en ${texto}` : `hace ${texto}`
}

export function soles(monto: number): string {
  return 'S/ ' + monto.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 912345678 -> 912 345 678 */
export function celular(numero: string): string {
  return numero.replace(/(\d{3})(\d{3})(\d{3})/, '$1 $2 $3')
}
