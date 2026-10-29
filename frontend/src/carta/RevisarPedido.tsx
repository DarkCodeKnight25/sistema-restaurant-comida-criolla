import { useEffect, type ReactNode } from 'react'
import { CircleAlert, Trash2, X } from 'lucide-react'
import { ErrorApi } from '../api/cliente'
import { precioCartel, soles } from '../lib/formato'
import type { Carrito } from './useCarta'

interface Props {
  carrito: Carrito
  titulo: string
  accion: string
  enviando: boolean
  error: Error | null
  cerrar: () => void
  enviar: () => void
  /** Campos extra (datos del cliente para llevar o delivery). */
  extra?: ReactNode
  puedeEnviar?: boolean
}

/** Revisión final antes de mandar a cocina: cantidades, notas y lo que se haya agotado mientras tanto. */
export function RevisarPedido({ carrito, titulo, accion, enviando, error, cerrar, enviar, extra, puedeEnviar = true }: Props) {
  useEffect(() => {
    const tecla = (e: KeyboardEvent) => e.key === 'Escape' && cerrar()
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [cerrar])

  const hayAgotados = carrito.agotados.length > 0
  const agotadosDelError = error instanceof ErrorApi && error.estado === 409

  return (
    <div className="hoja-fondo" onMouseDown={(e) => e.target === e.currentTarget && cerrar()}>
      <div className="hoja-revisar" role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="hoja-revisar-cabeza">
          <h2>{titulo}</h2>
          <button className="btn btn-texto" onClick={cerrar} aria-label="Cerrar">
            <X size={20} />
          </button>
        </div>

        <ul className="lineas">
          {carrito.detalle.map((l) => (
            <li key={l.platoId} className={l.plato.disponible ? '' : 'linea-agotada'}>
              <span className="lineas-cantidad rotulo">{l.cantidad}</span>
              <span className="lineas-texto">
                <strong>{l.plato.nombre}</strong>
                {!l.plato.disponible ? <span className="lineas-agotado">Se acabó</span> : l.nota ? <span>{l.nota}</span> : null}
              </span>
              <span className="num lineas-precio">{precioCartel(l.plato.precio * l.cantidad)}</span>
              <button className="btn btn-texto btn-chico" onClick={() => carrito.poner(l.platoId, 0)} aria-label={`Quitar ${l.plato.nombre}`}>
                <Trash2 size={16} />
              </button>
            </li>
          ))}
        </ul>

        {(hayAgotados || agotadosDelError) && (
          <div className="error-bloque" role="alert">
            <CircleAlert size={16} />
            <span>
              {agotadosDelError ? error!.message : 'Un plato se acabó mientras elegías.'}
              {hayAgotados && (
                <button className="btn btn-texto btn-chico" onClick={() => carrito.quitar(carrito.agotados)}>
                  Quitar lo agotado
                </button>
              )}
            </span>
          </div>
        )}
        {error && !agotadosDelError && (
          <div className="error-bloque" role="alert">
            <CircleAlert size={16} /> {error.message}
          </div>
        )}

        {extra}

        <div className="hoja-revisar-pie">
          <div>
            <span>Total</span>
            <strong className="num">{soles(carrito.total)}</strong>
          </div>
          <button
            className="btn btn-primario btn-grande"
            disabled={enviando || hayAgotados || carrito.cantidad === 0 || !puedeEnviar}
            onClick={enviar}
          >
            {enviando ? 'Enviando…' : accion}
          </button>
        </div>
      </div>
    </div>
  )
}
