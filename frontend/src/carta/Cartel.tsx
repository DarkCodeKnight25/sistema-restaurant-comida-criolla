import { useEffect, useRef, useState, type MouseEvent } from 'react'
import { Minus, Plus } from 'lucide-react'
import type { Plato } from '../api/tipos'
import { precioCartel } from '../lib/formato'

interface Props {
  plato: Plato
  cantidad: number
  grande?: boolean
  abrir: () => void
  sumar: () => void
  restar: () => void
}

/**
 * El precio rotulado «salta» hasta la barra del pedido: un clon viaja en 260 ms desde el plato.
 * Si el usuario prefiere menos movimiento, no hay viaje.
 */
export function volarPrecio(origen: HTMLElement | null) {
  const barra = document.querySelector('.barra-pedido')
  if (!origen || !barra || matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const a = origen.getBoundingClientRect()
  const b = barra.getBoundingClientRect()
  const clon = origen.cloneNode(true) as HTMLElement
  clon.classList.add('precio-vuela')
  Object.assign(clon.style, { left: `${a.left}px`, top: `${a.top}px`, width: `${a.width}px` })
  document.body.appendChild(clon)
  const dx = b.left + 40 - a.left
  const dy = b.top + 14 - a.top
  clon
    .animate(
      [
        { transform: 'translate(0, 0) scale(1)', opacity: 1 },
        { transform: `translate(${dx}px, ${dy}px) scale(0.5)`, opacity: 0.2 },
      ],
      { duration: 260, easing: 'cubic-bezier(0.22, 1, 0.36, 1)' },
    )
    .finished.finally(() => clon.remove())
}

/**
 * Un plato como tablilla esmaltada colgada: miniatura cuadrada, nombre, descripción y el precio rotulado.
 * Cuando se termina, una franja azul pintada «SE TERMINÓ» la cruza; si pasa en vivo, la franja entra pintándose.
 */
export function Cartel({ plato, cantidad, abrir, sumar, restar }: Props) {
  const agotado = !plato.disponible
  const antes = useRef(plato.disponible)
  const precio = useRef<HTMLSpanElement>(null)
  const [recienAgotado, setRecienAgotado] = useState(false)

  useEffect(() => {
    if (antes.current && !plato.disponible) {
      setRecienAgotado(true)
      const t = window.setTimeout(() => setRecienAgotado(false), 600)
      antes.current = plato.disponible
      return () => window.clearTimeout(t)
    }
    antes.current = plato.disponible
  }, [plato.disponible])

  const agregar = (e: MouseEvent) => {
    e.stopPropagation()
    volarPrecio(precio.current)
    sumar()
  }

  return (
    <article
      className={'tablilla' + (plato.foto ? '' : ' sin-foto') + (agotado ? ' agotado' : '') + (cantidad > 0 ? ' en-pedido' : '')}
      aria-label={`${plato.nombre}, ${precioCartel(plato.precio)}${agotado ? ', se terminó' : ''}`}
    >
      <button className="tablilla-cuerpo" onClick={abrir} disabled={agotado}>
        {plato.foto && (
          <span className="tablilla-foto">
            <img src={'/' + plato.foto} alt="" loading="lazy" decoding="async" />
          </span>
        )}
        <span className="tablilla-texto">
          <span className="tablilla-nombre">{plato.nombre}</span>
          {plato.descripcion && <span className="tablilla-desc">{plato.descripcion}</span>}
        </span>
        <span className="tablilla-precio rotulo" ref={precio}>
          {precioCartel(plato.precio)}
        </span>
      </button>

      {agotado ? (
        <span className={'franja' + (recienAgotado ? ' franja-entra' : '')} aria-hidden="true">
          <span className="rotulo">SE TERMINÓ</span>
        </span>
      ) : cantidad > 0 ? (
        <div className="contador">
          <button onClick={restar} aria-label={`Quitar un ${plato.nombre}`}>
            <Minus size={16} />
          </button>
          <span className="rotulo">{cantidad}</span>
          <button onClick={agregar} aria-label={`Agregar otro ${plato.nombre}`}>
            <Plus size={16} />
          </button>
        </div>
      ) : (
        <button className="agregar" onClick={agregar} aria-label={`Agregar ${plato.nombre}`}>
          <Plus size={20} />
        </button>
      )}
    </article>
  )
}

/** El letrero rojo de la puerta: el plato del día en letras de rotulista con sombra amarilla. */
export function LetreroDelDia({ plato, cantidad, abrir, sumar }: Props) {
  const precio = useRef<HTMLSpanElement>(null)
  const agotado = !plato.disponible
  return (
    <article className={'letrero' + (agotado ? ' agotado' : '')} aria-label={`Plato del día: ${plato.nombre}, ${precioCartel(plato.precio)}`}>
      <div className="letrero-lata">
        <h3 className="letrero-plato rotulo rotulo-sombra">
          <span className="letrero-hoy">Hoy:</span> {plato.nombre}
        </h3>
        {plato.descripcion && <p className="letrero-desc">{plato.descripcion}</p>}
        <div className="letrero-pie">
          <span className="letrero-precio rotulo" ref={precio}>
            {precioCartel(plato.precio)}
          </span>
          {agotado ? (
            <span className="letrero-agotado rotulo">Se terminó por hoy</span>
          ) : (
            <button
              className="btn btn-amarillo"
              onClick={() => {
                volarPrecio(precio.current)
                sumar()
              }}
            >
              <Plus size={18} /> {cantidad > 0 ? `Otro más (${cantidad})` : 'Pedir el del día'}
            </button>
          )}
        </div>
      </div>
      {plato.foto && (
        <button className="letrero-foto" onClick={abrir} aria-label={`Ver ${plato.nombre}`}>
          <img src={'/' + plato.foto} alt="" />
        </button>
      )}
    </article>
  )
}
