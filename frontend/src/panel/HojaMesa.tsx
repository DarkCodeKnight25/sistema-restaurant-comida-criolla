import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ExternalLink, Minus, Plus, X } from 'lucide-react'
import QRCode from 'qrcode'
import { api } from '../api/cliente'
import type { Carta, Mesa, MesaPublica, Pedido } from '../api/tipos'
import { ESTADO_PEDIDO, numeroPedido, precioCartel, soles } from '../lib/formato'
import { useSesion } from '../lib/sesion'
import { useTostada } from '../ui/Tostada'

/** Detalle de una mesa: su QR, lo que lleva pedido y, para el mozo, tomar un pedido desde aquí. */
export function HojaMesa({ mesa, cerrar }: { mesa: Mesa; cerrar: () => void }) {
  const { usuario } = useSesion()
  const [qr, setQr] = useState<string>('')
  const [tomando, setTomando] = useState(false)
  const url = `${location.origin}/m/${mesa.token}`

  const detalle = useQuery({
    queryKey: ['mesa', mesa.token],
    queryFn: () => api<MesaPublica>(`/api/publico/mesas/${mesa.token}`),
  })

  useEffect(() => {
    QRCode.toDataURL(url, { margin: 1, width: 360, color: { dark: '#1B2A33', light: '#FFFFFF' } }).then(setQr)
  }, [url])

  useEffect(() => {
    const tecla = (e: KeyboardEvent) => e.key === 'Escape' && cerrar()
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [cerrar])

  const puedeTomar = usuario?.rol === 'MOZO' || usuario?.rol === 'ADMIN' || usuario?.rol === 'CAJA'

  return (
    <div className="hoja-fondo-panel" onMouseDown={(e) => e.target === e.currentTarget && cerrar()}>
      <div className="hoja-panel" role="dialog" aria-modal="true" aria-label={`Mesa ${mesa.numero}`}>
        <div className="hoja-panel-cabeza">
          <h2>
            <span className="rotulo">Mesa {mesa.numero}</span> <span className="gris">{mesa.zona} · {mesa.capacidad} personas</span>
          </h2>
          <button className="btn btn-texto" onClick={cerrar} aria-label="Cerrar">
            <X size={20} />
          </button>
        </div>
        <div className="hoja-panel-cuerpo">
          {tomando ? (
            <TomarPedido mesa={mesa} listo={() => setTomando(false)} />
          ) : (
            <>
              <div className="mesa-qr">
                {qr ? <img src={qr} width={150} height={150} alt={`Código QR de la mesa ${mesa.numero}`} /> : <span className="esqueleto" style={{ width: 150, height: 150 }} />}
                <div>
                  <p>El comensal escanea este código para ver la carta y pedir desde su celular.</p>
                  <a className="btn btn-chico" href={url} target="_blank" rel="noreferrer">
                    <ExternalLink size={15} /> Abrir la carta de esta mesa
                  </a>
                </div>
              </div>

              {detalle.data && detalle.data.pedidos.length > 0 ? (
                <>
                  <ul className="mesa-pedidos">
                    {detalle.data.pedidos.map((p: Pedido) => (
                      <li key={p.id}>
                        <div>
                          <span className="rotulo">{numeroPedido(p.numero)}</span>
                          <span className="estado">{ESTADO_PEDIDO[p.estado]}</span>
                          <span className="num">{soles(p.total)}</span>
                        </div>
                        <p>
                          {p.items.map((i) => `${i.cantidad}× ${i.nombre}`).join(', ')}
                        </p>
                      </li>
                    ))}
                  </ul>
                  <p className="mesa-total">
                    Consumo de la mesa <strong className="num">{soles(detalle.data.total)}</strong>
                  </p>
                </>
              ) : (
                <p className="vacio">La mesa está libre.</p>
              )}
              {puedeTomar && (
                <button className="btn btn-primario" onClick={() => setTomando(true)}>
                  <Plus size={16} /> Tomar pedido para la mesa {mesa.numero}
                </button>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}

/** El mozo arma un pedido rápido; entra ya confirmado a la cocina. */
function TomarPedido({ mesa, listo }: { mesa: Mesa; listo: () => void }) {
  const carta = useQuery({ queryKey: ['carta-admin'], queryFn: () => api<Carta>('/api/carta') })
  const [cantidades, setCantidades] = useState<Record<number, number>>({})
  const cliente = useQueryClient()
  const tostada = useTostada()

  const platos = carta.data?.platos.filter((p) => p.activo) ?? []
  const total = platos.reduce((s, p) => s + (cantidades[p.id] ?? 0) * p.precio, 0)
  const lineas = Object.entries(cantidades).filter(([, c]) => c > 0).map(([id, c]) => ({ platoId: Number(id), cantidad: c }))

  const enviar = useMutation({
    mutationFn: () => api<Pedido>('/api/pedidos', { metodo: 'POST', cuerpo: { mesaId: mesa.id, lineas } }),
    onSuccess: (p) => {
      tostada({ texto: `${numeroPedido(p.numero)} enviado a cocina para la mesa ${mesa.numero}.` })
      cliente.invalidateQueries({ queryKey: ['mesa', mesa.token] })
      listo()
    },
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })

  return (
    <div className="tomar">
      <ul className="tomar-lista">
        {platos.map((p) => {
          const c = cantidades[p.id] ?? 0
          return (
            <li key={p.id} className={p.disponible ? '' : 'tomar-agotado'}>
              <span>
                <strong>{p.nombre}</strong>
                <span className="num gris">{p.disponible ? precioCartel(p.precio) : 'Se acabó'}</span>
              </span>
              <span className="tomar-contador">
                <button disabled={c === 0} onClick={() => setCantidades({ ...cantidades, [p.id]: c - 1 })} aria-label={`Quitar ${p.nombre}`}>
                  <Minus size={16} />
                </button>
                <span className="num">{c}</span>
                <button disabled={!p.disponible} onClick={() => setCantidades({ ...cantidades, [p.id]: c + 1 })} aria-label={`Agregar ${p.nombre}`}>
                  <Plus size={16} />
                </button>
              </span>
            </li>
          )
        })}
      </ul>
      <div className="tomar-pie">
        <button className="btn btn-texto" onClick={listo}>
          Volver
        </button>
        <button className="btn btn-primario" disabled={lineas.length === 0 || enviar.isPending} onClick={() => enviar.mutate()}>
          Enviar a cocina · <span className="num">{soles(total)}</span>
        </button>
      </div>
    </div>
  )
}
