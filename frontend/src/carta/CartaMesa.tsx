import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ChevronDown, CircleAlert, ReceiptText } from 'lucide-react'
import { api, ErrorApi } from '../api/cliente'
import type { MesaPublica, Pedido } from '../api/tipos'
import { ESTADO_PEDIDO, numeroPedido, plural, soles } from '../lib/formato'
import { useVivo } from '../lib/vivo'
import { useTostada } from '../ui/Tostada'
import { useCarrito, useCartaPublica, useLocal } from './useCarta'
import { VistaCarta } from './VistaCarta'
import { RevisarPedido } from './RevisarPedido'
import { Marca } from './Marca'

/** La carta que abre el QR de una mesa: el comensal pide, sigue su pedido y pide la cuenta. */
export function CartaMesa() {
  const { token = '' } = useParams()
  const local = useLocal()
  const carta = useCartaPublica()
  const carrito = useCarrito(carta.data?.platos)
  const cliente = useQueryClient()
  const tostada = useTostada()
  const [revisando, setRevisando] = useState(false)
  const [ticket, setTicket] = useState<Pedido | null>(null)
  const [verMesa, setVerMesa] = useState(false)

  const mesa = useQuery({
    queryKey: ['mesa', token],
    queryFn: () => api<MesaPublica>(`/api/publico/mesas/${token}`),
  })
  useVivo(['/topic/carta', `/topic/mesa/${token}`], [['carta'], ['mesa', token]])

  const enviar = useMutation({
    mutationFn: () =>
      api<Pedido>(`/api/publico/mesas/${token}/pedidos`, {
        metodo: 'POST',
        cuerpo: { lineas: carrito.lineas.map((l) => ({ platoId: l.platoId, cantidad: l.cantidad, nota: l.nota })) },
      }),
    onSuccess: (p) => {
      setRevisando(false)
      carrito.vaciar()
      setTicket(p)
      cliente.invalidateQueries({ queryKey: ['mesa', token] })
    },
    onError: (e) => {
      if (e instanceof ErrorApi && e.estado === 409) cliente.invalidateQueries({ queryKey: ['carta'] })
    },
  })

  const cuenta = useMutation({
    mutationFn: () => api<void>(`/api/publico/mesas/${token}/cuenta`, { metodo: 'POST' }),
    onSuccess: () => {
      tostada({ texto: 'Avisamos a caja: ya vienen con tu cuenta.' })
      cliente.invalidateQueries({ queryKey: ['mesa', token] })
    },
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })

  if (mesa.error) {
    return (
      <div className="pantalla-mensaje">
        <Marca />
        <h1>Este QR no es de una mesa</h1>
        <p>{mesa.error.message} Pide ayuda al mozo o haz tu pedido para llevar.</p>
        <a className="btn btn-primario" href="/">
          Ver la carta para llevar
        </a>
      </div>
    )
  }
  if (!carta.data || !mesa.data) return <CargandoCarta />

  const m = mesa.data
  const pedidosVivos = m.pedidos.filter((p) => p.estado !== 'ANULADO')
  const pidioCuenta = m.estadoCuenta === 'CUENTA'

  return (
    <>
      <VistaCarta
        carta={carta.data}
        carrito={carrito}
        cabecera={
          <>
            <div className="cabecera-fila">
              <Marca />
              <div className="cabecera-local">
                <strong>{local.data?.nombre ?? 'La Sazón de Doña Meche'}</strong>
                <span>{local.data?.tipo}</span>
              </div>
              <span className="etiqueta etiqueta-mesa">Mesa {m.numero}</span>
            </div>
          </>
        }
        arriba={
          pedidosVivos.length > 0 && (
            <section className="estado-mesa" aria-label="Tu mesa">
              <button className="estado-mesa-resumen" onClick={() => setVerMesa(!verMesa)} aria-expanded={verMesa}>
                <span>
                  <strong>Tu mesa</strong> · {plural(pedidosVivos.length, 'pedido', 'pedidos')} ·{' '}
                  <span className="num">{soles(m.total)}</span>
                </span>
                <span className="estado-mesa-ultimo">
                  {pidioCuenta ? 'Cuenta pedida' : ESTADO_PEDIDO[pedidosVivos[pedidosVivos.length - 1].estado]}
                  <ChevronDown size={16} />
                </span>
              </button>
              {verMesa && (
                <ol className="estado-mesa-lista">
                  {pedidosVivos.map((p) => (
                    <li key={p.id}>
                      <div className="estado-mesa-pedido">
                        <span className="rotulo">{numeroPedido(p.numero)}</span>
                        <EstadoPedido estado={p.estado} />
                      </div>
                      <ul>
                        {p.items
                          .filter((i) => i.estado !== 'ANULADO')
                          .map((i) => (
                            <li key={i.id}>
                              <span className="num">{i.cantidad}×</span> {i.nombre}
                              {i.estado === 'LISTO' && <span className="estado estado-info">Listo</span>}
                            </li>
                          ))}
                      </ul>
                    </li>
                  ))}
                </ol>
              )}
            </section>
          )
        }
        barra={
          <div className="barra-pedido">
            {carrito.cantidad > 0 ? (
              <button className="barra-pedido-btn" onClick={() => setRevisando(true)}>
                <span className="barra-cantidad rotulo">{carrito.cantidad}</span>
                <span className="barra-texto">
                  <strong>Tu pedido</strong>
                  <span className="num">{soles(carrito.total)}</span>
                </span>
                <span className="barra-accion">Revisar y enviar</span>
              </button>
            ) : pedidosVivos.length > 0 ? (
              <div className="barra-mesa">
                <span>
                  {pidioCuenta ? 'Ya vienen con tu cuenta' : 'Total de la mesa'} ·{' '}
                  <strong className="num">{soles(m.total)}</strong>
                </span>
                <button className="btn btn-primario" disabled={pidioCuenta || cuenta.isPending} onClick={() => cuenta.mutate()}>
                  <ReceiptText size={18} /> {pidioCuenta ? 'Cuenta pedida' : 'Pedir la cuenta'}
                </button>
              </div>
            ) : (
              <p className="barra-ayuda">Toca un plato para agregarlo. Tu pedido va directo a la cocina.</p>
            )}
          </div>
        }
      />

      {revisando && (
        <RevisarPedido
          carrito={carrito}
          titulo={`Pedido para la mesa ${m.numero}`}
          accion="Enviar a cocina"
          enviando={enviar.isPending}
          error={enviar.error}
          cerrar={() => setRevisando(false)}
          enviar={() => enviar.mutate()}
        />
      )}

      {ticket && <Ticket pedido={ticket} cerrar={() => setTicket(null)} />}
      {local.data?.demo && <p className="nota-demo-carta">Demo: el restaurante, la carta y los pedidos son ficticios.</p>}
    </>
  )
}

function EstadoPedido({ estado }: { estado: Pedido['estado'] }) {
  const clase =
    estado === 'ENVIADO' ? 'estado-alerta' : estado === 'LISTO' ? 'estado-info' : estado === 'SERVIDO' ? 'estado-ok' : ''
  return <span className={'estado ' + clase}>{ESTADO_PEDIDO[estado]}</span>
}

/** Al enviar, la barra se convierte en un ticket con el número del pedido a plumón. */
function Ticket({ pedido, cerrar }: { pedido: Pedido; cerrar: () => void }) {
  return (
    <div className="ticket-fondo" onClick={cerrar}>
      <div className="ticket" role="status" aria-live="polite">
        <span className="ticket-titulo">Pedido enviado a cocina</span>
        <span className="ticket-numero rotulo">{numeroPedido(pedido.numero)}</span>
        <span className="ticket-texto">
          El mozo lo confirma en un momento. Lo verás arriba en «Tu mesa» cuando esté listo.
        </span>
        <button className="btn btn-primario" onClick={cerrar}>
          Seguir viendo la carta
        </button>
      </div>
    </div>
  )
}

export function CargandoCarta() {
  return (
    <div className="carta carta-cargando" aria-busy="true">
      <div className="carta-cabecera">
        <span className="esqueleto" style={{ width: 160, height: 22 }} />
      </div>
      <div className="carta-cuerpo">
        {Array.from({ length: 4 }, (_, i) => (
          <div key={i} className="cartel esqueleto-cartel" />
        ))}
      </div>
    </div>
  )
}

export function ErrorCarga({ mensaje }: { mensaje: string }) {
  return (
    <div className="error-bloque">
      <CircleAlert size={16} /> {mensaje}
    </div>
  )
}
