import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Check } from 'lucide-react'
import { api } from '../api/cliente'
import type { EstadoPedido, Pedido } from '../api/tipos'
import { numeroPedido, soles, TIPO } from '../lib/formato'
import { useVivo } from '../lib/vivo'
import { Marca } from './Marca'
import './carta.css'

const PASOS_LLEVAR: { estado: EstadoPedido[]; texto: string }[] = [
  { estado: ['ENVIADO'], texto: 'Recibido' },
  { estado: ['CONFIRMADO', 'EN_PREPARACION'], texto: 'En cocina' },
  { estado: ['LISTO'], texto: 'Listo para recoger' },
  { estado: ['ENTREGADO'], texto: 'Entregado' },
]
const PASOS_DELIVERY: { estado: EstadoPedido[]; texto: string }[] = [
  { estado: ['ENVIADO'], texto: 'Recibido' },
  { estado: ['CONFIRMADO', 'EN_PREPARACION'], texto: 'En cocina' },
  { estado: ['LISTO'], texto: 'Listo, sale en breve' },
  { estado: ['EN_CAMINO'], texto: 'En camino' },
  { estado: ['ENTREGADO'], texto: 'Entregado' },
]

/** El cliente de para llevar o delivery sigue su pedido en vivo con su código. */
export function Seguimiento() {
  const { codigo = '' } = useParams()
  const pedido = useQuery({ queryKey: ['seguimiento', codigo], queryFn: () => api<Pedido>(`/api/publico/pedidos/${codigo}`) })
  useVivo([`/topic/seguimiento/${codigo}`], [['seguimiento', codigo]])

  if (pedido.error) {
    return (
      <div className="pantalla-mensaje">
        <Marca />
        <h1>No encontramos ese pedido</h1>
        <p>{pedido.error.message}</p>
        <Link className="btn btn-primario" to="/">
          Volver a la carta
        </Link>
      </div>
    )
  }
  const p = pedido.data
  if (!p) return <div className="pantalla-mensaje" aria-busy="true" />

  const pasos = p.tipo === 'DELIVERY' ? PASOS_DELIVERY : PASOS_LLEVAR
  const actual = p.estado === 'ANULADO' ? -1 : pasos.findIndex((s) => s.estado.includes(p.estado))

  return (
    <div className="seguimiento">
      <header className="cabecera-fila">
        <Marca />
        <div className="cabecera-local">
          <strong>La Sazón de Doña Meche</strong>
          <span>{TIPO[p.tipo]}</span>
        </div>
      </header>

      <div className="seguimiento-ticket">
        <span>Tu pedido</span>
        <strong className="rotulo">{numeroPedido(p.numero)}</strong>
        <span className="num seguimiento-codigo">Código {p.codigo}</span>
      </div>

      {p.estado === 'ANULADO' ? (
        <p className="error-bloque">El pedido fue anulado. Si es un error, llámanos.</p>
      ) : (
        <ol className="pasos" aria-label="Estado del pedido">
          {pasos.map((s, i) => (
            <li key={s.texto} className={i < actual ? 'hecho' : i === actual ? 'actual' : ''} aria-current={i === actual ? 'step' : undefined}>
              <span className="pasos-marca">{i < actual ? <Check size={14} /> : null}</span>
              {s.texto}
            </li>
          ))}
        </ol>
      )}

      <ul className="lineas lineas-seguimiento">
        {p.items.map((i) => (
          <li key={i.id}>
            <span className="lineas-cantidad rotulo">{i.cantidad}</span>
            <span className="lineas-texto">
              <strong>{i.nombre}</strong>
              {i.nota && <span>{i.nota}</span>}
            </span>
            <span className="num lineas-precio">{soles(i.precioUnit * i.cantidad)}</span>
          </li>
        ))}
      </ul>
      <p className="seguimiento-total">
        Total a pagar al {p.tipo === 'DELIVERY' ? 'recibir' : 'recoger'}: <strong className="num">{soles(p.total)}</strong>
        {p.pagado && <span className="estado estado-ok">Pagado</span>}
      </p>
      <p className="seguimiento-ayuda">Esta página se actualiza sola. Guárdala para ver cómo va tu pedido.</p>
      <Link className="btn btn-texto" to="/">
        Hacer otro pedido
      </Link>
    </div>
  )
}
