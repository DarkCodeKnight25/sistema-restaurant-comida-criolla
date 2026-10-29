import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CircleAlert, X } from 'lucide-react'
import QRCode from 'qrcode'
import { api } from '../api/cliente'
import type { Cierre, Cuenta, Local, Medio, Mesa, Pedido, Resumen } from '../api/tipos'
import { hoyLima } from '../lib/fechas'
import { MEDIO, numeroPedido, plural, soles, TIPO } from '../lib/formato'
import { useTostada } from '../ui/Tostada'

const MEDIOS: Medio[] = ['EFECTIVO', 'YAPE', 'PLIN', 'TARJETA']

/** Caja: ¿cuánto cobro ahora y cuadra el día? */
export function Caja() {
  const resumen = useQuery({ queryKey: ['resumen'], queryFn: () => api<Resumen>('/api/caja/resumen') })
  const mesas = useQuery({ queryKey: ['mesas'], queryFn: () => api<Mesa[]>('/api/mesas') })
  const pedidos = useQuery({ queryKey: ['pedidos', 'todos'], queryFn: () => api<Pedido[]>('/api/pedidos?todos=true') })
  const cierres = useQuery({ queryKey: ['resumen', 'cierres'], queryFn: () => api<Cierre[]>('/api/caja/cierres') })
  const [cobrando, setCobrando] = useState<{ tipo: 'mesa'; atencionId: number } | { tipo: 'pedido'; pedido: Pedido } | null>(null)

  const abiertas = useMemo(
    () =>
      (mesas.data ?? [])
        .filter((m) => m.atencion)
        .sort((a, b) => (a.atencion!.estado === 'CUENTA' ? -1 : 0) - (b.atencion!.estado === 'CUENTA' ? -1 : 0)),
    [mesas.data],
  )
  const externos = useMemo(
    () => (pedidos.data ?? []).filter((p) => p.tipo !== 'MESA' && !p.pagado && p.estado !== 'ANULADO'),
    [pedidos.data],
  )
  const r = resumen.data
  const piden = abiertas.filter((m) => m.atencion!.estado === 'CUENTA').length

  return (
    <main className="pagina">
      <header className="fila-titulo">
        <div>
          <h1>Caja</h1>
          <p className="hallazgo">
            {!r
              ? 'Cargando…'
              : [
                  piden > 0 && `${plural(piden, 'mesa pide', 'mesas piden')} la cuenta`,
                  r.mesasAbiertas > 0 && `${plural(r.mesasAbiertas, 'mesa abierta', 'mesas abiertas')}`,
                  externos.length > 0 && `${plural(externos.length, 'pedido externo', 'pedidos externos')} sin cobrar`,
                ]
                  .filter(Boolean)
                  .join(' · ') || 'Todo cobrado.'}
          </p>
        </div>
      </header>

      {resumen.error && (
        <div className="error-bloque">
          <CircleAlert size={16} /> {resumen.error.message}
        </div>
      )}

      {r && (
        <section className="kpis" aria-label="Cobrado hoy">
          <div className="kpi kpi-principal">
            <span>Cobrado hoy</span>
            <strong className="num rotulo kpi-rotulo">{soles(r.total)}</strong>
          </div>
          <div className="kpi">
            <span>Tickets</span>
            <strong className="num">{r.tickets}</strong>
          </div>
          <div className="kpi">
            <span>Ticket promedio</span>
            <strong className="num">{soles(r.ticketPromedio)}</strong>
          </div>
          {MEDIOS.map((m) => (
            <div className="kpi kpi-medio" key={m}>
              <span>{MEDIO[m]}</span>
              <strong className={'num' + (r.porMedio[m] === 0 ? ' gris' : '')}>{soles(r.porMedio[m])}</strong>
            </div>
          ))}
        </section>
      )}

      <div className="caja-rejilla">
        <section className="tarjeta" aria-labelledby="t-cobrar">
          <h2 id="t-cobrar" className="titulo-bloque">
            Por cobrar
          </h2>
          {abiertas.length === 0 && externos.length === 0 ? (
            <p className="vacio">No hay nada por cobrar.</p>
          ) : (
            <ul className="por-cobrar">
              {abiertas.map((m) => (
                <li key={m.id}>
                  <span className="rotulo por-cobrar-mesa">Mesa {m.numero}</span>
                  <span className={'estado ' + (m.atencion!.estado === 'CUENTA' ? 'estado-alerta' : '')}>
                    {m.atencion!.estado === 'CUENTA' ? 'Pide la cuenta' : 'Comiendo'}
                  </span>
                  <span className="num por-cobrar-monto">{soles(m.atencion!.consumo)}</span>
                  <button className="btn btn-chico btn-primario" onClick={() => setCobrando({ tipo: 'mesa', atencionId: m.atencion!.id })}>
                    Cobrar
                  </button>
                </li>
              ))}
              {externos.map((p) => (
                <li key={p.id}>
                  <span className="rotulo por-cobrar-mesa">{numeroPedido(p.numero)}</span>
                  <span className="estado">
                    {TIPO[p.tipo]} · {p.clienteNombre?.split(' ')[0]}
                  </span>
                  <span className="num por-cobrar-monto">{soles(p.total)}</span>
                  <button className="btn btn-chico btn-primario" onClick={() => setCobrando({ tipo: 'pedido', pedido: p })}>
                    Cobrar
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="tarjeta" aria-labelledby="t-horas">
          <h2 id="t-horas" className="titulo-bloque">
            Cobrado por hora, hoy
          </h2>
          {r ? <BarrasPorHora valores={r.ventasPorHora} /> : <span className="esqueleto" style={{ height: 180 }} />}
        </section>

        <section className="tarjeta" aria-labelledby="t-top">
          <h2 id="t-top" className="titulo-bloque">
            Lo más pedido hoy
          </h2>
          {r && r.masVendidos.length > 0 ? (
            <ol className="top-platos">
              {r.masVendidos.map((p, i) => (
                <li key={p.nombre}>
                  <span className="top-nombre">{p.nombre}</span>
                  <span className="num">{p.cantidad}</span>
                  <span className="top-barra" style={{ width: `${(p.cantidad / r.masVendidos[0].cantidad) * 100}%` }} data-primero={i === 0 || undefined} />
                </li>
              ))}
            </ol>
          ) : (
            <p className="vacio">Todavía no hay ventas hoy.</p>
          )}
        </section>

        <CierreDelDia resumen={r} historial={cierres.data ?? []} />
      </div>

      {cobrando?.tipo === 'mesa' && <CobrarMesa atencionId={cobrando.atencionId} cerrar={() => setCobrando(null)} />}
      {cobrando?.tipo === 'pedido' && <CobrarPedido pedido={cobrando.pedido} cerrar={() => setCobrando(null)} />}
    </main>
  )
}

/** Barras de cobro por hora en el horario del local; el dato principal lleva el único color del panel. */
function BarrasPorHora({ valores }: { valores: number[] }) {
  const primero = valores.findIndex((v) => v > 0)
  const desde = Math.min(11, primero < 0 ? 11 : primero)
  const hasta = Math.max(17, valores.findLastIndex((v) => v > 0) + 1)
  const horas = Array.from({ length: hasta - desde }, (_, i) => desde + i)
  const max = Math.max(...horas.map((h) => valores[h]), 1)
  const tope = Math.ceil(max / 200) * 200
  const W = 560
  const H = 180
  const arriba = 12
  const izq = 44
  const ancho = (W - izq) / horas.length
  const lineas = [0, tope / 2, tope]
  return (
    <svg viewBox={`0 -${arriba} ${W} ${H + 26 + arriba}`} className="grafico" role="img" aria-label="Cobrado por hora">
      {lineas.map((v) => (
        <g key={v}>
          <line x1={izq} x2={W} y1={H - (v / tope) * H} y2={H - (v / tope) * H} className="grafico-linea" />
          <text x={izq - 6} y={H - (v / tope) * H + 4} textAnchor="end" className="grafico-texto">
            {v}
          </text>
        </g>
      ))}
      {horas.map((h, i) => {
        const v = valores[h]
        const alto = (v / tope) * H
        return (
          <g key={h}>
            {v > 0 && <rect x={izq + i * ancho + 4} y={H - alto} width={ancho - 8} height={alto} className="grafico-barra"><title>{`${h}:00 · S/ ${v}`}</title></rect>}
            <text x={izq + i * ancho + ancho / 2} y={H + 18} textAnchor="middle" className="grafico-texto">
              {h}h
            </text>
          </g>
        )
      })}
    </svg>
  )
}

function CierreDelDia({ resumen, historial }: { resumen: Resumen | undefined; historial: Cierre[] }) {
  const [contado, setContado] = useState('')
  const [seguro, setSeguro] = useState(false)
  const cliente = useQueryClient()
  const tostada = useTostada()
  const esperado = resumen?.porMedio.EFECTIVO ?? 0
  const diferencia = contado === '' ? null : Number(contado) - esperado

  const cerrar = useMutation({
    mutationFn: () => api<Cierre>('/api/caja/cierres', { metodo: 'POST', cuerpo: { fecha: hoyLima(), efectivoContado: Number(contado) } }),
    onSuccess: () => {
      tostada({ texto: 'Caja del día cerrada.' })
      cliente.invalidateQueries({ queryKey: ['resumen'] })
      setSeguro(false)
    },
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })

  const ultimos = [...historial.slice(-13), ...(resumen ? [{ fecha: resumen.fecha, total: resumen.total } as Cierre] : [])]
  const max = Math.max(...ultimos.map((c) => c.total), 1)

  return (
    <section className="tarjeta" aria-labelledby="t-cierre">
      <h2 id="t-cierre" className="titulo-bloque">
        Cierre del día
      </h2>
      {resumen?.cierre ? (
        <dl className="cierre-hecho">
          <div>
            <dt>Cerrado por</dt>
            <dd>{resumen.cierre.cerradoPor}</dd>
          </div>
          <div>
            <dt>Efectivo contado</dt>
            <dd className="num">{soles(resumen.cierre.efectivoContado)}</dd>
          </div>
          <div>
            <dt>Diferencia</dt>
            <dd className={'num' + (resumen.cierre.diferencia < 0 ? ' texto-peligro' : '')}>{soles(resumen.cierre.diferencia)}</dd>
          </div>
        </dl>
      ) : (
        <div className="cierre-form">
          <p>
            Efectivo esperado en caja: <strong className="num rotulo cierre-esperado">{soles(esperado)}</strong>
          </p>
          <label className="campo">
            <span>Efectivo contado</span>
            <input className="entrada num" inputMode="decimal" value={contado} onChange={(e) => setContado(e.target.value.replace(/[^\d.]/g, ''))} placeholder="0.00" />
          </label>
          {diferencia !== null && (
            <p className={diferencia < 0 ? 'texto-peligro' : diferencia > 0 ? 'texto-alerta' : 'texto-ok'}>
              {diferencia === 0 ? 'Cuadra exacto.' : diferencia < 0 ? `Faltan ${soles(-diferencia)}.` : `Sobran ${soles(diferencia)}.`}
            </p>
          )}
          {!seguro ? (
            <button className="btn btn-peligro" disabled={contado === ''} onClick={() => setSeguro(true)}>
              Cerrar la caja de hoy
            </button>
          ) : (
            <div className="confirmar-cierre" role="alert">
              <p>Después de cerrar no se pueden registrar más cobros de hoy.</p>
              <button className="btn btn-peligro" disabled={cerrar.isPending} onClick={() => cerrar.mutate()}>
                Sí, cerrar caja
              </button>
              <button className="btn btn-texto" onClick={() => setSeguro(false)}>
                No
              </button>
            </div>
          )}
        </div>
      )}
      {ultimos.length > 1 && (
        <div className="historial">
          <h3>Últimos 14 días</h3>
          <div className="historial-barras" role="img" aria-label="Ventas de los últimos 14 días">
            {ultimos.map((c, i) => (
              <span key={c.fecha} title={`${c.fecha}: ${soles(c.total)}`} style={{ height: `${(c.total / max) * 100}%` }} data-hoy={i === ultimos.length - 1 || undefined} />
            ))}
          </div>
        </div>
      )}
    </section>
  )
}

function useCobro(cerrarHoja: () => void) {
  const cliente = useQueryClient()
  const tostada = useTostada()
  return { cliente, tostada, cerrarHoja }
}

function CobrarMesa({ atencionId, cerrar }: { atencionId: number; cerrar: () => void }) {
  const cuenta = useQuery({ queryKey: ['cuenta', atencionId], queryFn: () => api<Cuenta>(`/api/caja/cuentas/${atencionId}`) })
  const { cliente, tostada } = useCobro(cerrar)
  const [medio, setMedio] = useState<Medio>('YAPE')
  const [monto, setMonto] = useState('')
  const [recibido, setRecibido] = useState('')
  const [partes, setPartes] = useState(1)

  const saldo = cuenta.data?.saldo ?? 0
  useEffect(() => {
    if (cuenta.data) setMonto((partes > 1 ? Math.ceil((saldo / partes) * 10) / 10 : saldo).toFixed(2))
  }, [cuenta.data, partes]) // eslint-disable-line react-hooks/exhaustive-deps

  const cobrar = useMutation({
    mutationFn: () =>
      api(`/api/caja/cuentas/${atencionId}/pagos`, {
        metodo: 'POST',
        cuerpo: { medio, monto: Number(monto), recibido: medio === 'EFECTIVO' && recibido ? Number(recibido) : null },
      }),
    onSuccess: () => {
      cliente.invalidateQueries({ queryKey: ['cuenta', atencionId] })
      cliente.invalidateQueries({ queryKey: ['mesas'] })
      cliente.invalidateQueries({ queryKey: ['resumen'] })
      const queda = saldo - Number(monto)
      tostada({ texto: queda > 0.001 ? `Cobrado. Queda ${soles(queda)}.` : `Mesa ${cuenta.data?.mesa} pagada y libre.` })
      setRecibido('')
      if (queda <= 0.001) cerrar()
    },
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })

  const vuelto = medio === 'EFECTIVO' && recibido ? Number(recibido) - Number(monto) : null

  return (
    <HojaCobro titulo={cuenta.data ? `Cuenta de la mesa ${cuenta.data.mesa}` : 'Cuenta'} cerrar={cerrar}>
      {cuenta.data && (
        <>
          <ul className="cuenta-lineas">
            {agrupar(cuenta.data.pedidos).map((l) => (
              <li key={l.nombre + l.precio}>
                <span className="num">{l.cantidad}×</span>
                <span>{l.nombre}</span>
                <span className="num">{soles(l.cantidad * l.precio)}</span>
              </li>
            ))}
          </ul>
          <dl className="cuenta-totales">
            <div>
              <dt>Total</dt>
              <dd className="num">{soles(cuenta.data.total)}</dd>
            </div>
            {cuenta.data.pagado > 0 && (
              <div>
                <dt>Ya pagado</dt>
                <dd className="num">{soles(cuenta.data.pagado)}</dd>
              </div>
            )}
            <div className="cuenta-saldo">
              <dt>Por cobrar</dt>
              <dd className="num">{soles(saldo)}</dd>
            </div>
          </dl>
          <FormCobro
            medio={medio}
            setMedio={setMedio}
            monto={monto}
            setMonto={setMonto}
            recibido={recibido}
            setRecibido={setRecibido}
            vuelto={vuelto}
            partes={partes}
            setPartes={setPartes}
            enviando={cobrar.isPending}
            cobrar={() => cobrar.mutate()}
          />
        </>
      )}
    </HojaCobro>
  )
}

function CobrarPedido({ pedido, cerrar }: { pedido: Pedido; cerrar: () => void }) {
  const { cliente, tostada } = useCobro(cerrar)
  const [medio, setMedio] = useState<Medio>('YAPE')
  const [recibido, setRecibido] = useState('')
  const cobrar = useMutation({
    mutationFn: () =>
      api(`/api/caja/pedidos/${pedido.id}/pagos`, {
        metodo: 'POST',
        cuerpo: { medio, recibido: medio === 'EFECTIVO' && recibido ? Number(recibido) : null },
      }),
    onSuccess: () => {
      cliente.invalidateQueries({ queryKey: ['pedidos'] })
      cliente.invalidateQueries({ queryKey: ['resumen'] })
      tostada({ texto: `${numeroPedido(pedido.numero)} cobrado.` })
      cerrar()
    },
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })
  const vuelto = medio === 'EFECTIVO' && recibido ? Number(recibido) - pedido.total : null
  return (
    <HojaCobro titulo={`${TIPO[pedido.tipo]} ${numeroPedido(pedido.numero)} · ${pedido.clienteNombre ?? ''}`} cerrar={cerrar}>
      <ul className="cuenta-lineas">
        {pedido.items.map((i) => (
          <li key={i.id}>
            <span className="num">{i.cantidad}×</span>
            <span>{i.nombre}</span>
            <span className="num">{soles(i.cantidad * i.precioUnit)}</span>
          </li>
        ))}
      </ul>
      <dl className="cuenta-totales">
        <div className="cuenta-saldo">
          <dt>Por cobrar</dt>
          <dd className="num">{soles(pedido.total)}</dd>
        </div>
      </dl>
      <FormCobro
        medio={medio}
        setMedio={setMedio}
        monto={pedido.total.toFixed(2)}
        recibido={recibido}
        setRecibido={setRecibido}
        vuelto={vuelto}
        enviando={cobrar.isPending}
        cobrar={() => cobrar.mutate()}
      />
    </HojaCobro>
  )
}

function FormCobro(p: {
  medio: Medio
  setMedio: (m: Medio) => void
  monto: string
  setMonto?: (m: string) => void
  recibido: string
  setRecibido: (r: string) => void
  vuelto: number | null
  partes?: number
  setPartes?: (n: number) => void
  enviando: boolean
  cobrar: () => void
}) {
  const local = useQuery({ queryKey: ['local'], queryFn: () => api<Local>('/api/publico/local'), staleTime: 600_000 })
  const [qr, setQr] = useState('')
  const digital = p.medio === 'YAPE' || p.medio === 'PLIN'
  useEffect(() => {
    if (!digital) return
    QRCode.toDataURL(`DEMO ${p.medio} ${local.data?.yapeDemo ?? ''} S/ ${p.monto}`, { margin: 1, width: 260, color: { dark: '#1B2A33', light: '#FFFFFF' } }).then(setQr)
  }, [digital, p.medio, p.monto, local.data])

  const montoValido = Number(p.monto) > 0
  const efectivoCorto = p.vuelto !== null && p.vuelto < 0

  return (
    <div className="form-cobro">
      <div className="medios" role="radiogroup" aria-label="Medio de pago">
        {MEDIOS.map((m) => (
          <button key={m} role="radio" aria-checked={p.medio === m} onClick={() => p.setMedio(m)}>
            {MEDIO[m]}
          </button>
        ))}
      </div>
      {p.setPartes && (
        <label className="campo campo-linea">
          <span>Dividir entre</span>
          <select className="entrada" value={p.partes} onChange={(e) => p.setPartes!(Number(e.target.value))}>
            {[1, 2, 3, 4, 5, 6].map((n) => (
              <option key={n} value={n}>
                {n === 1 ? 'Una sola cuenta' : `${n} personas`}
              </option>
            ))}
          </select>
        </label>
      )}
      {p.setMonto && (
        <label className="campo">
          <span>Monto a cobrar ahora</span>
          <input className="entrada num" inputMode="decimal" value={p.monto} onChange={(e) => p.setMonto!(e.target.value.replace(/[^\d.]/g, ''))} />
        </label>
      )}
      {p.medio === 'EFECTIVO' && (
        <label className="campo">
          <span>Recibido</span>
          <input className="entrada num" inputMode="decimal" value={p.recibido} onChange={(e) => p.setRecibido(e.target.value.replace(/[^\d.]/g, ''))} placeholder="Ej.: 100" />
          {p.vuelto !== null && (
            <span className={efectivoCorto ? 'error' : 'vuelto'}>
              {efectivoCorto ? 'No alcanza para el monto.' : `Vuelto: ${soles(p.vuelto)}`}
            </span>
          )}
        </label>
      )}
      {digital && (
        <div className="qr-pago">
          {qr && <img src={qr} width={130} height={130} alt={`QR de ${MEDIO[p.medio]} simulado`} />}
          <p>
            El cliente escanea con {MEDIO[p.medio]} y paga <strong className="num">{soles(Number(p.monto) || 0)}</strong>. Confirma
            en el celular de la caja antes de registrar.
            {local.data?.demo && <span className="gris"> QR de demostración: no cobra de verdad.</span>}
          </p>
        </div>
      )}
      <button className="btn btn-primario btn-grande" disabled={p.enviando || !montoValido || efectivoCorto} onClick={p.cobrar}>
        Registrar cobro de {soles(Number(p.monto) || 0)}
      </button>
    </div>
  )
}

function HojaCobro({ titulo, cerrar, children }: { titulo: string; cerrar: () => void; children: React.ReactNode }) {
  useEffect(() => {
    const tecla = (e: KeyboardEvent) => e.key === 'Escape' && cerrar()
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [cerrar])
  return (
    <div className="hoja-fondo-panel" onMouseDown={(e) => e.target === e.currentTarget && cerrar()}>
      <div className="hoja-panel" role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="hoja-panel-cabeza">
          <h2>{titulo}</h2>
          <button className="btn btn-texto" onClick={cerrar} aria-label="Cerrar">
            <X size={20} />
          </button>
        </div>
        <div className="hoja-panel-cuerpo">{children}</div>
      </div>
    </div>
  )
}

/** Junta los mismos platos de varios pedidos en una línea de la cuenta. */
function agrupar(pedidos: Pedido[]) {
  const mapa = new Map<string, { nombre: string; precio: number; cantidad: number }>()
  pedidos.flatMap((p) => p.items).filter((i) => i.estado !== 'ANULADO').forEach((i) => {
    const k = i.nombre + '|' + i.precioUnit
    const actual = mapa.get(k) ?? { nombre: i.nombre, precio: i.precioUnit, cantidad: 0 }
    actual.cantidad += i.cantidad
    mapa.set(k, actual)
  })
  return [...mapa.values()]
}
