// Cliente HTTP: agrega el token, traduce los errores RFC 9457 y avisa cuando el servidor gratuito está despertando.

const BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? ''
const CLAVE_TOKEN = 'mn.token'

export class ErrorApi extends Error {
  readonly estado: number
  readonly campos: Record<string, string>

  constructor(estado: number, mensaje: string, campos: Record<string, string> = {}) {
    super(mensaje)
    this.estado = estado
    this.campos = campos
  }
}

export function leerToken(): string | null {
  try {
    return localStorage.getItem(CLAVE_TOKEN)
  } catch {
    return null
  }
}

export function guardarToken(token: string | null) {
  try {
    if (token) localStorage.setItem(CLAVE_TOKEN, token)
    else localStorage.removeItem(CLAVE_TOKEN)
  } catch {
    // Sin almacenamiento (modo privado): la sesión dura lo que la pestaña.
  }
}

/** Avisos de "servidor despertando": Render apaga el plan gratuito tras 15 minutos sin uso. */
type Oyente = (despertando: boolean) => void
const oyentes = new Set<Oyente>()
let pendientesLentas = 0

export function escucharDespertar(fn: Oyente) {
  oyentes.add(fn)
  return () => {
    oyentes.delete(fn)
  }
}

function avisar() {
  oyentes.forEach((fn) => fn(pendientesLentas > 0))
}

let alExpirar: (() => void) | null = null
export function alExpirarSesion(fn: () => void) {
  alExpirar = fn
}

export async function api<T>(ruta: string, opciones: { metodo?: string; cuerpo?: unknown } = {}): Promise<T> {
  const headers: Record<string, string> = {}
  const token = leerToken()
  if (token && !ruta.startsWith('/api/publico')) headers.Authorization = `Bearer ${token}`
  if (opciones.cuerpo !== undefined) headers['Content-Type'] = 'application/json'

  let lenta = false
  const temporizador = window.setTimeout(() => {
    lenta = true
    pendientesLentas++
    avisar()
  }, 3500)

  let respuesta: Response
  try {
    respuesta = await fetch(BASE + ruta, {
      method: opciones.metodo ?? 'GET',
      headers,
      body: opciones.cuerpo !== undefined ? JSON.stringify(opciones.cuerpo) : undefined,
    })
  } catch {
    throw new ErrorApi(0, 'No hay conexión con el servidor. Revisa tu internet e inténtalo otra vez.')
  } finally {
    window.clearTimeout(temporizador)
    if (lenta) {
      pendientesLentas--
      avisar()
    }
  }

  if (respuesta.status === 204) return undefined as T
  const texto = await respuesta.text()
  const datos = texto ? JSON.parse(texto) : undefined

  if (!respuesta.ok) {
    if (respuesta.status === 401 && token && !ruta.startsWith('/api/auth/login')) {
      guardarToken(null)
      alExpirar?.()
    }
    const mensaje =
      datos?.detail ??
      (respuesta.status === 403
        ? 'Tu usuario no tiene permiso para hacer esto.'
        : 'Algo falló en el servidor. Inténtalo de nuevo en un momento.')
    throw new ErrorApi(respuesta.status, mensaje, datos?.campos ?? {})
  }
  return datos as T
}
