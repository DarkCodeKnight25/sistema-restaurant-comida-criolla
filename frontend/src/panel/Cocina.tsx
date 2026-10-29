import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Check, CircleAlert } from 'lucide-react'
import { api } from '../api/cliente'
import type { Carta, Estacion, EstadoItem, Item, Pedido } from '../api/tipos'
import { ahoraLima, minutosEntre } from '../lib/fechas'
import { numeroPedido, plural, TIPO } from '../lib/formato'
import { useTostada } from '../ui/Tostada'

/** Un pedido que espera más que esto lleva el anillo rojo. */
const MINUTOS_ATRASO = 15

const SIGUIENTE: Partial<Record<EstadoItem, EstadoItem>> = {
  PENDIENTE: 'EN_PREPARACION',
  EN_PREPARACION: 'LISTO',
}

/** Pantalla de cocina: una columna por estación, el número de pedido como titular, lo atrasado con anillo. */
export function Cocina() {
  const carta = useQuery({ queryKey: ['carta-admin'], queryFn: () => api<Carta>('/api/carta') })
  const pedidos = useQuery({ queryKey: ['pedidos'], queryFn: () => api<Pedido[]>('/api/pedidos') })
  const ahora = useReloj()
  const [soloEstacion, setSoloEstacion] = useState<number | null>(null)

  const estaciones = carta.data?.estaciones ?? []
  const visibles = soloEstacion ? estaciones.filter((e) => e.id === soloEstacion) : estaciones

  if (pedidos.error || carta.error) {
    return (
      <main className="pagina">
        <div className="error-bloque">
          <CircleAlert size={16} /> {(pedidos.error ?? carta.error)!.message}
        </div>
      </main>
    )
  }

  return (
    <main className="cocina">
      <div className="cocina-filtro" role="tablist" aria-label="Estación">
        <button role="tab" aria-selected={soloEstacion === null} onClick={() => setSoloEstacion(null)}>
          Todas
        </button>
        {estaciones.map((e) => (
          <button key={e.id} role="tab" aria-selected={soloEstacion === e.id} onClick={() => setSoloEstacion(e.id)}>
            {e.nombre}
          </button>
        ))}
      </div>
      <div className="cocina-columnas" style={{ ['--columnas' as string]: visibles.length || 1 }}>
        {visibles.map((e) => (
          <ColumnaEstacion key={e.id} estacion={e} pedidos={pedidos.data ?? []} ahora={ahora} cargando={pedidos.isPending} />
        ))}
      </div>
    </main>
  )
}

function ColumnaEstacion({ estacion, pedidos, ahora, cargando }: { estacion: Estacion; pedidos: Pedido[]; ahora: string; cargando: boolean }) {
  // Pedidos ya confirmados que tienen trabajo pendiente en esta estación, del más antiguo al más nuevo.
  const cola = useMemo(
    () =>
      pedidos
        .filter((p) => p.estado !== 'ENVIADO' && p.estado !== 'ANULADO')
        .map((p) => ({ pedido: p, items: p.items.filter((i) => i.estacionId === estacion.id && (i.estado === 'PENDIENTE' || i.estado === 'EN_PREPARACION')) }))
        .filter((x) => x.items.length > 0)
        .sort((a, b) => a.pedido.creadoEn.localeCompare(b.pedido.creadoEn)),
    [pedidos, estacion.id],
  )
  const platos = cola.reduce((s, x) => s + x.items.reduce((t, i) => t + i.cantidad, 0), 0)
  // La carga se ve como densidad del rayado del encabezado.
  const carga = platos >= 10 ? 'alta' : platos >= 5 ? 'media' : platos > 0 ? 'baja' : 'nula'

  return (
    <section className="estacion" style={{ ['--color' as string]: estacion.color }} aria-labelledby={`est-${estacion.id}`}>
      <header className={`estacion-cabeza carga-${carga}`}>
        <h2 id={`est-${estacion.id}`}>{estacion.nombre}</h2>
        <span className="num">{plural(platos, 'plato', 'platos')}</span>
      </header>
      <div className="estacion-cola">
        {cargando && <span className="esqueleto" style={{ height: 180 }} />}
        {!cargando && cola.length === 0 && <p className="vacio">Nada pendiente en {estacion.nombre.toLowerCase()}.</p>}
        {cola.map(({ pedido, items }) => (
          <Comanda key={pedido.id} pedido={pedido} items={items} estacion={estacion} ahora={ahora} />
        ))}
      </div>
    </section>
  )
}

function Comanda({ pedido, items, estacion, ahora }: { pedido: Pedido; items: Item[]; estacion: Estacion; ahora: string }) {
  const tostada = useTostada()
  const espera = Math.max(0, minutosEntre(pedido.creadoEn, ahora))
  const atrasado = espera >= MINUTOS_ATRASO

  const avanzar = useMutation({
    mutationFn: ({ id, estado }: { id: number; estado: EstadoItem }) =>
      api<Pedido>(`/api/pedidos/items/${id}`, { metodo: 'PATCH', cuerpo: { estado } }),
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })
  const todo = useMutation({
    mutationFn: () => api<Pedido>(`/api/pedidos/${pedido.id}/estaciones/${estacion.id}/listo`, { metodo: 'POST' }),
    onSuccess: () => tostada({ texto: `${numeroPedido(pedido.numero)}: ${estacion.nombre.toLowerCase()} listo.` }),
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })

  const destino = pedido.tipo === 'MESA' ? `Mesa ${pedido.mesa}` : `${TIPO[pedido.tipo]} · ${pedido.clienteNombre?.split(' ')[0] ?? ''}`

  return (
    <article className={'comanda' + (atrasado ? ' atrasada' : '')} aria-label={`Pedido ${pedido.numero}, ${destino}`}>
      <header className="comanda-cabeza">
        <span className="comanda-numero rotulo">{numeroPedido(pedido.numero)}</span>
        <span className="comanda-destino">
          <strong>{destino}</strong>
          <span className={'num' + (atrasado ? ' comanda-tarde' : '')}>{espera === 0 ? 'recién' : `hace ${espera} min`}</span>
        </span>
      </header>
      <ul className="comanda-items">
        {items.map((i) => (
          <li key={i.id}>
            <button
              className={'comanda-item ' + i.estado.toLowerCase()}
              disabled={avanzar.isPending}
              onClick={() => SIGUIENTE[i.estado] && avanzar.mutate({ id: i.id, estado: SIGUIENTE[i.estado]! })}
              aria-label={`${i.cantidad} ${i.nombre}, ${i.estado === 'PENDIENTE' ? 'pendiente: tocar para empezar' : 'en preparación: tocar para marcar listo'}`}
            >
              <span className="comanda-cantidad rotulo">{i.cantidad}</span>
              <span className="comanda-plato">
                <strong>{i.nombre}</strong>
                {i.nota && <span className="comanda-nota">{i.nota}</span>}
              </span>
              <span className="comanda-estado">{i.estado === 'PENDIENTE' ? 'Empezar' : 'Listo'}</span>
            </button>
          </li>
        ))}
      </ul>
      {pedido.nota && <p className="comanda-nota-pedido">{pedido.nota}</p>}
      <button className="btn btn-primario comanda-listo" disabled={todo.isPending} onClick={() => todo.mutate()}>
        <Check size={18} /> Todo {estacion.nombre.toLowerCase()} listo
      </button>
    </article>
  )
}

/** Hora de Lima que avanza cada 20 s para recalcular las esperas. */
function useReloj() {
  const [ahora, setAhora] = useState(ahoraLima())
  useEffect(() => {
    const t = window.setInterval(() => setAhora(ahoraLima()), 20_000)
    return () => window.clearInterval(t)
  }, [])
  return ahora
}
