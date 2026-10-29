import { useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Bike, Check, CircleAlert, ShoppingBag } from 'lucide-react'
import { api } from '../api/cliente'
import type { Mesa, Pedido } from '../api/tipos'
import { ahoraLima, minutosEntre } from '../lib/fechas'
import { numeroPedido, soles, TIPO } from '../lib/formato'
import { useTostada } from '../ui/Tostada'
import { HojaMesa } from './HojaMesa'

type EstadoMesa = 'libre' | 'por-confirmar' | 'listo' | 'cuenta' | 'ocupada'

function estadoDe(m: Mesa): EstadoMesa {
  const a = m.atencion
  if (!a) return 'libre'
  if (a.porConfirmar > 0) return 'por-confirmar'
  if (a.estado === 'CUENTA') return 'cuenta'
  if (a.listos > 0) return 'listo'
  return 'ocupada'
}

const TEXTO_ESTADO: Record<EstadoMesa, string> = {
  libre: 'Libre',
  'por-confirmar': 'Pedido por confirmar',
  listo: 'Platos listos para llevar',
  cuenta: 'Pidió la cuenta',
  ocupada: 'Comiendo',
}

/** Vista del mozo: qué mesa lo necesita, qué confirmar y qué llevar. */
export function Mesas() {
  const mesas = useQuery({ queryKey: ['mesas'], queryFn: () => api<Mesa[]>('/api/mesas') })
  const pedidos = useQuery({ queryKey: ['pedidos'], queryFn: () => api<Pedido[]>('/api/pedidos') })
  const [abierta, setAbierta] = useState<number | null>(null)

  const porConfirmar = useMemo(() => pedidos.data?.filter((p) => p.estado === 'ENVIADO') ?? [], [pedidos.data])
  const paraServir = useMemo(
    () => pedidos.data?.filter((p) => p.tipo === 'MESA' && p.items.some((i) => i.estado === 'LISTO')) ?? [],
    [pedidos.data],
  )
  const externos = useMemo(
    () => pedidos.data?.filter((p) => p.tipo !== 'MESA' && p.estado !== 'ENVIADO') ?? [],
    [pedidos.data],
  )
  const ocupadas = mesas.data?.filter((m) => m.atencion).length ?? 0
  const mesa = mesas.data?.find((m) => m.id === abierta)

  return (
    <main className="pagina">
      <header className="fila-titulo">
        <div>
          <h1>Mesas</h1>
          <p className="hallazgo">
            {mesas.isPending
              ? 'Cargando…'
              : [
                  `${ocupadas} de ${mesas.data?.length ?? 0} ocupadas`,
                  porConfirmar.length > 0 && `${porConfirmar.length} por confirmar`,
                  paraServir.length > 0 && `${paraServir.length} con platos listos`,
                ]
                  .filter(Boolean)
                  .join(' · ')}
          </p>
        </div>
      </header>

      {(mesas.error || pedidos.error) && (
        <div className="error-bloque">
          <CircleAlert size={16} /> {(mesas.error ?? pedidos.error)!.message}
        </div>
      )}

      <div className="mesas-rejilla-pagina">
        <section aria-labelledby="t-salon">
          <h2 id="t-salon" className="titulo-bloque">
            Salón y terraza
          </h2>
          <ul className="mesas">
            {mesas.data?.map((m) => {
              const est = estadoDe(m)
              return (
                <li key={m.id}>
                  <button className={`mesa mesa-${est}`} onClick={() => setAbierta(m.id)} aria-label={`Mesa ${m.numero}, ${TEXTO_ESTADO[est]}`}>
                    <span className="mesa-numero rotulo">{m.numero}</span>
                    <span className="mesa-zona">{m.zona}</span>
                    <span className="mesa-estado">{TEXTO_ESTADO[est]}</span>
                    {m.atencion && <span className="mesa-consumo num">{soles(m.atencion.consumo)}</span>}
                  </button>
                </li>
              )
            })}
          </ul>
        </section>

        <aside className="colas">
          <Cola titulo="Por confirmar" cantidad={porConfirmar.length} vacio="Nada por confirmar.">
            {porConfirmar.map((p) => (
              <PedidoPorConfirmar key={p.id} pedido={p} />
            ))}
          </Cola>
          <Cola titulo="Listo para llevar a la mesa" cantidad={paraServir.length} vacio="Nada listo por ahora.">
            {paraServir.map((p) => (
              <PedidoParaServir key={p.id} pedido={p} />
            ))}
          </Cola>
          <Cola titulo="Para llevar y delivery" cantidad={externos.length} vacio="Sin pedidos externos en curso.">
            {externos.map((p) => (
              <PedidoExterno key={p.id} pedido={p} />
            ))}
          </Cola>
        </aside>
      </div>

      {mesa && <HojaMesa mesa={mesa} cerrar={() => setAbierta(null)} />}
    </main>
  )
}

function Cola({ titulo, cantidad, vacio, children }: { titulo: string; cantidad: number; vacio: string; children: React.ReactNode }) {
  return (
    <section className="cola">
      <h2 className="titulo-bloque">
        {titulo} <span className="num gris">{cantidad}</span>
      </h2>
      {cantidad === 0 ? <p className="vacio">{vacio}</p> : <ul className="cola-lista">{children}</ul>}
    </section>
  )
}

function destino(p: Pedido) {
  return p.tipo === 'MESA' ? `Mesa ${p.mesa}` : `${TIPO[p.tipo]} · ${p.clienteNombre ?? ''}`
}

function PedidoPorConfirmar({ pedido }: { pedido: Pedido }) {
  const tostada = useTostada()
  const [anulando, setAnulando] = useState(false)
  const accion = useMutation({
    mutationFn: (que: 'confirmar' | 'anular') => api<Pedido>(`/api/pedidos/${pedido.id}/${que}`, { metodo: 'POST' }),
    onSuccess: (p, que) => tostada({ texto: `${numeroPedido(p.numero)} ${que === 'confirmar' ? 'enviado a cocina' : 'anulado'}.` }),
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })
  const espera = Math.max(0, minutosEntre(pedido.creadoEn, ahoraLima()))
  return (
    <li className="cola-pedido">
      <div className="cola-pedido-cabeza">
        <span className="rotulo">{numeroPedido(pedido.numero)}</span>
        <strong>{destino(pedido)}</strong>
        <span className="num gris">hace {espera} min</span>
      </div>
      <ul className="cola-items">
        {pedido.items.map((i) => (
          <li key={i.id}>
            <span className="num">{i.cantidad}×</span> {i.nombre}
            {i.nota && <em> · {i.nota}</em>}
          </li>
        ))}
      </ul>
      <div className="cola-acciones">
        <button className="btn btn-primario" disabled={accion.isPending} onClick={() => accion.mutate('confirmar')}>
          <Check size={16} /> Confirmar · {soles(pedido.total)}
        </button>
        {!anulando ? (
          <button className="btn btn-texto btn-chico cola-anular" onClick={() => setAnulando(true)}>
            Anular
          </button>
        ) : (
          <span className="confirmar-anular">
            ¿Anular {numeroPedido(pedido.numero)}?
            <button className="btn btn-peligro btn-chico" disabled={accion.isPending} onClick={() => accion.mutate('anular')}>
              Sí, anular
            </button>
            <button className="btn btn-texto btn-chico" onClick={() => setAnulando(false)}>
              No
            </button>
          </span>
        )}
      </div>
    </li>
  )
}

function PedidoParaServir({ pedido }: { pedido: Pedido }) {
  const tostada = useTostada()
  const servir = useMutation({
    mutationFn: () => api<Pedido>(`/api/pedidos/${pedido.id}/servir`, { metodo: 'POST' }),
    onSuccess: () => tostada({ texto: `Servido en la mesa ${pedido.mesa}.` }),
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })
  const listos = pedido.items.filter((i) => i.estado === 'LISTO')
  return (
    <li className="cola-pedido">
      <div className="cola-pedido-cabeza">
        <span className="rotulo">{numeroPedido(pedido.numero)}</span>
        <strong>Mesa {pedido.mesa}</strong>
      </div>
      <ul className="cola-items">
        {listos.map((i) => (
          <li key={i.id}>
            <span className="num">{i.cantidad}×</span> {i.nombre}
          </li>
        ))}
      </ul>
      <div className="cola-acciones">
        <button className="btn btn-primario" disabled={servir.isPending} onClick={() => servir.mutate()}>
          <Check size={16} /> Llevé a la mesa
        </button>
      </div>
    </li>
  )
}

function PedidoExterno({ pedido }: { pedido: Pedido }) {
  const tostada = useTostada()
  const accion = useMutation({
    mutationFn: (que: 'despachar' | 'entregar') => api<Pedido>(`/api/pedidos/${pedido.id}/${que}`, { metodo: 'POST' }),
    onSuccess: (p) => tostada({ texto: `${numeroPedido(p.numero)}: ${p.estado === 'EN_CAMINO' ? 'salió a reparto' : 'entregado'}.` }),
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })
  const listo = pedido.estado === 'LISTO'
  return (
    <li className="cola-pedido">
      <div className="cola-pedido-cabeza">
        <span className="rotulo">{numeroPedido(pedido.numero)}</span>
        <strong>
          {pedido.tipo === 'DELIVERY' ? <Bike size={15} /> : <ShoppingBag size={15} />} {pedido.clienteNombre}
        </strong>
        <span className={'estado ' + (listo ? 'estado-info' : pedido.estado === 'EN_CAMINO' ? 'estado-alerta' : '')}>
          {listo ? 'Listo' : pedido.estado === 'EN_CAMINO' ? 'En camino' : 'En cocina'}
        </span>
      </div>
      {pedido.direccion && <p className="cola-direccion">{pedido.direccion}</p>}
      <div className="cola-acciones">
        <span className={pedido.pagado ? 'estado estado-ok' : 'estado estado-alerta'}>
          {pedido.pagado ? 'Pagado' : `Cobrar ${soles(pedido.total)} en caja`}
        </span>
        {pedido.tipo === 'DELIVERY' && listo && (
          <button className="btn btn-chico" disabled={accion.isPending} onClick={() => accion.mutate('despachar')}>
            Salió a reparto
          </button>
        )}
        {((pedido.tipo === 'LLEVAR' && listo) || pedido.estado === 'EN_CAMINO') && (
          <button className="btn btn-chico btn-primario" disabled={accion.isPending || !pedido.pagado} onClick={() => accion.mutate('entregar')}>
            Entregado
          </button>
        )}
      </div>
    </li>
  )
}
