import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { X } from 'lucide-react'
import type { Carta, Plato } from '../api/tipos'
import { precioCartel } from '../lib/formato'
import type { Carrito } from './useCarta'
import { Cartel, LetreroDelDia } from './Cartel'
import { CREDITOS } from './creditos'
import './carta.css'

interface Props {
  carta: Carta
  carrito: Carrito
  cabecera: ReactNode
  /** Contenido entre la cabecera y la carta (estado de la mesa, aviso de demo...). */
  arriba?: ReactNode
  barra: ReactNode
}

/** La carta como el letrero de la puerta: el plato del día arriba y cada sección como tablillas esmaltadas. */
export function VistaCarta({ carta, carrito, cabecera, arriba, barra }: Props) {
  const [abierto, setAbierto] = useState<Plato | null>(null)
  const [activa, setActiva] = useState<number | null>(null)
  const secciones = useRef(new Map<number, HTMLElement>())

  const porCategoria = useMemo(
    () =>
      carta.categorias
        .map((c) => ({ categoria: c, platos: carta.platos.filter((p) => p.categoriaId === c.id) }))
        .filter((g) => g.platos.length > 0),
    [carta],
  )
  const colorDe = (estacionId: number) => carta.estaciones.find((e) => e.id === estacionId)?.color

  // La etiqueta activa sigue a la sección que se está leyendo.
  useEffect(() => {
    const obs = new IntersectionObserver(
      (entradas) => {
        const visible = entradas.filter((e) => e.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0]
        if (visible) setActiva(Number((visible.target as HTMLElement).dataset.cat))
      },
      { rootMargin: '-120px 0px -60% 0px' },
    )
    secciones.current.forEach((el) => obs.observe(el))
    return () => obs.disconnect()
  }, [porCategoria])

  const cantidadDe = (id: number) => carrito.lineas.find((l) => l.platoId === id)?.cantidad ?? 0

  return (
    <div className="carta">
      <header className="carta-cabecera">{cabecera}</header>

      <nav className="carta-secciones" aria-label="Secciones de la carta">
        {porCategoria.map(({ categoria, platos }) => (
          <button
            key={categoria.id}
            className="etiqueta"
            aria-current={activa === categoria.id ? 'true' : undefined}
            style={{ ['--color' as string]: colorDe(platos[0].estacionId) }}
            onClick={() => secciones.current.get(categoria.id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
          >
            {categoria.nombre}
          </button>
        ))}
      </nav>

      {arriba}

      <main className="carta-cuerpo">
        {porCategoria.map(({ categoria, platos }, gi) => (
          <section
            key={categoria.id}
            className="carta-seccion"
            data-cat={categoria.id}
            ref={(el) => {
              if (el) secciones.current.set(categoria.id, el)
            }}
            aria-labelledby={`cat-${categoria.id}`}
          >
            {gi === 0 && platos.length === 1 ? (
              <>
                <h2 id={`cat-${categoria.id}`} className="sr-only">
                  {categoria.nombre}
                </h2>
                <LetreroDelDia
                  plato={platos[0]}
                  cantidad={cantidadDe(platos[0].id)}
                  abrir={() => setAbierto(platos[0])}
                  sumar={() => carrito.poner(platos[0].id, cantidadDe(platos[0].id) + 1, carrito.lineas.find((l) => l.platoId === platos[0].id)?.nota)}
                  restar={() => carrito.poner(platos[0].id, cantidadDe(platos[0].id) - 1)}
                />
              </>
            ) : (
              <>
                <h2 id={`cat-${categoria.id}`} className="carta-seccion-titulo">
                  <span className="rotulo">{categoria.nombre}</span>
                  <span className="etiqueta" style={{ ['--color' as string]: colorDe(platos[0].estacionId) }}>
                    {carta.estaciones.find((e) => e.id === platos[0].estacionId)?.nombre}
                  </span>
                </h2>
                <div className="tablillas">
                  {platos.map((p) => (
                    <Cartel
                      key={p.id}
                      plato={p}
                      cantidad={cantidadDe(p.id)}
                      abrir={() => setAbierto(p)}
                      sumar={() => carrito.poner(p.id, cantidadDe(p.id) + 1, carrito.lineas.find((l) => l.platoId === p.id)?.nota)}
                      restar={() => carrito.poner(p.id, cantidadDe(p.id) - 1, carrito.lineas.find((l) => l.platoId === p.id)?.nota)}
                    />
                  ))}
                </div>
              </>
            )}
          </section>
        ))}
        <footer className="carta-pie">
          <p>Hecho por <a href="https://github.com/DarkCodeKnight25">David Vega</a>. La Sazón de Doña Meche es un local ficticio.</p>
          <details>
            <summary>Créditos de las fotos (Wikimedia Commons)</summary>
            <ul>
              {CREDITOS.map((c) => (
                <li key={c.url}>
                  <a href={c.url}>«{c.titulo}»</a>, {c.autor}, {c.licencia}.
                </li>
              ))}
            </ul>
            <p>Son fotos de referencia de cada plato, no del local.</p>
          </details>
        </footer>
      </main>

      {barra}

      {abierto && (
        <DetallePlato
          plato={abierto}
          cantidadInicial={cantidadDe(abierto.id) || 1}
          notaInicial={carrito.lineas.find((l) => l.platoId === abierto.id)?.nota ?? ''}
          cerrar={() => setAbierto(null)}
          guardar={(c, n) => {
            carrito.poner(abierto.id, c, n)
            setAbierto(null)
          }}
        />
      )}
    </div>
  )
}

function DetallePlato({
  plato,
  cantidadInicial,
  notaInicial,
  cerrar,
  guardar,
}: {
  plato: Plato
  cantidadInicial: number
  notaInicial: string
  cerrar: () => void
  guardar: (cantidad: number, nota: string) => void
}) {
  const [cantidad, setCantidad] = useState(cantidadInicial)
  const [nota, setNota] = useState(notaInicial)
  useEffect(() => {
    const tecla = (e: KeyboardEvent) => e.key === 'Escape' && cerrar()
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [cerrar])

  return (
    <div className="hoja-fondo" onMouseDown={(e) => e.target === e.currentTarget && cerrar()}>
      <div className="hoja-plato" role="dialog" aria-modal="true" aria-label={plato.nombre}>
        <button className="btn btn-texto hoja-cerrar" onClick={cerrar} aria-label="Cerrar">
          <X size={20} />
        </button>
        {plato.foto && <img className="hoja-foto" src={'/' + plato.foto} alt={plato.nombre} />}
        <div className="hoja-plato-texto">
          <h2>{plato.nombre}</h2>
          {plato.descripcion && <p>{plato.descripcion}</p>}
          <p className="rotulo hoja-precio">{precioCartel(plato.precio)}</p>
          <label className="campo">
            <span>¿Algún pedido especial?</span>
            <input
              className="entrada"
              maxLength={120}
              value={nota}
              onChange={(e) => setNota(e.target.value)}
              placeholder="Sin cebolla, bien picante…"
            />
          </label>
          <div className="hoja-plato-pie">
            <div className="contador-grande" role="group" aria-label="Cantidad">
              <button onClick={() => setCantidad(Math.max(1, cantidad - 1))} aria-label="Uno menos">
                −
              </button>
              <span className="rotulo">{cantidad}</span>
              <button onClick={() => setCantidad(Math.min(30, cantidad + 1))} aria-label="Uno más">
                +
              </button>
            </div>
            <button className="btn btn-primario btn-grande" onClick={() => guardar(cantidad, nota.trim())}>
              Agregar · <span className="num">{precioCartel(plato.precio * cantidad)}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
