import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Bike, ShoppingBag } from 'lucide-react'
import { api, ErrorApi } from '../api/cliente'
import type { Pedido, TipoPedido } from '../api/tipos'
import { soles } from '../lib/formato'
import { useVivo } from '../lib/vivo'
import { CargandoCarta, ErrorCarga } from './CartaMesa'
import { Marca } from './Marca'
import { RevisarPedido } from './RevisarPedido'
import { useCarrito, useCartaPublica, useLocal } from './useCarta'
import { VistaCarta } from './VistaCarta'

/** La carta pública para pedir para llevar o delivery. */
export function CartaExterna() {
  const local = useLocal()
  const carta = useCartaPublica()
  const carrito = useCarrito(carta.data?.platos)
  const cliente = useQueryClient()
  const navegar = useNavigate()
  const [tipo, setTipo] = useState<Exclude<TipoPedido, 'MESA'>>('LLEVAR')
  const [revisando, setRevisando] = useState(false)
  const [datos, setDatos] = useState({ nombre: '', celular: '', direccion: '', referencia: '' })
  const [tocado, setTocado] = useState(false)

  useVivo(['/topic/carta'], [['carta']])

  const errores = {
    nombre: !datos.nombre.trim() ? 'Escribe tu nombre.' : undefined,
    celular: !/^9\d{8}$/.test(datos.celular) ? 'El celular tiene 9 dígitos y empieza con 9.' : undefined,
    direccion: tipo === 'DELIVERY' && !datos.direccion.trim() ? 'Necesitamos la dirección.' : undefined,
  }
  const valido = !errores.nombre && !errores.celular && !errores.direccion

  const enviar = useMutation({
    mutationFn: () =>
      api<Pedido>('/api/publico/pedidos', {
        metodo: 'POST',
        cuerpo: {
          tipo,
          cliente: datos,
          lineas: carrito.lineas.map((l) => ({ platoId: l.platoId, cantidad: l.cantidad, nota: l.nota })),
        },
      }),
    onSuccess: (p) => {
      carrito.vaciar()
      navegar(`/pedido/${p.codigo}`)
    },
    onError: (e) => {
      if (e instanceof ErrorApi && e.estado === 409) cliente.invalidateQueries({ queryKey: ['carta'] })
    },
  })

  if (carta.error) {
    return (
      <div className="pantalla-mensaje">
        <ErrorCarga mensaje={carta.error.message} />
      </div>
    )
  }
  if (!carta.data) return <CargandoCarta />

  return (
    <>
      <VistaCarta
        carta={carta.data}
        carrito={carrito}
        cabecera={
          <div className="cabecera-fila">
            <Marca />
            <div className="cabecera-local">
              <strong>{local.data?.nombre ?? 'La Sazón de Doña Meche'}</strong>
              <span>{local.data ? `${local.data.horario} · ${local.data.distrito}` : ''}</span>
            </div>
            <Link to="/panel" className="btn btn-texto btn-chico">
              Personal
            </Link>
          </div>
        }
        arriba={
          <div className="modo-externo">
            <div className="selector-tipo" role="radiogroup" aria-label="Cómo quieres tu pedido">
              <button role="radio" aria-checked={tipo === 'LLEVAR'} onClick={() => setTipo('LLEVAR')}>
                <ShoppingBag size={18} /> Para llevar
              </button>
              <button role="radio" aria-checked={tipo === 'DELIVERY'} onClick={() => setTipo('DELIVERY')}>
                <Bike size={18} /> Delivery
              </button>
            </div>
            {local.data?.demo && (
              <p className="nota-demo-arriba">
                Demo de un sistema de restaurante criollo: el local, la carta y los pedidos son ficticios. Para probar el pedido desde una
                mesa, entra al panel del personal y abre el QR de cualquier mesa.
              </p>
            )}
          </div>
        }
        barra={
          <div className="barra-pedido">
            {carrito.cantidad > 0 ? (
              <button className="barra-pedido-btn" onClick={() => setRevisando(true)}>
                <span className="barra-cantidad rotulo">{carrito.cantidad}</span>
                <span className="barra-texto">
                  <strong>{tipo === 'LLEVAR' ? 'Para llevar' : 'Delivery'}</strong>
                  <span className="num">{soles(carrito.total)}</span>
                </span>
                <span className="barra-accion">Revisar y pedir</span>
              </button>
            ) : (
              <p className="barra-ayuda">
                Elige tus platos. {tipo === 'LLEVAR' ? 'Lo recoges en el local.' : 'Te lo llevamos a casa.'} Pagas al recibir.
              </p>
            )}
          </div>
        }
      />
      {revisando && (
        <RevisarPedido
          carrito={carrito}
          titulo={tipo === 'LLEVAR' ? 'Pedido para llevar' : 'Pedido por delivery'}
          accion="Hacer el pedido"
          enviando={enviar.isPending}
          error={enviar.error}
          cerrar={() => setRevisando(false)}
          puedeEnviar={valido}
          enviar={() => {
            setTocado(true)
            if (valido) enviar.mutate()
          }}
          extra={
            <div className="datos-cliente">
              <label className="campo">
                <span>Tu nombre</span>
                <input
                  className="entrada"
                  autoComplete="name"
                  value={datos.nombre}
                  aria-invalid={tocado && !!errores.nombre}
                  onChange={(e) => setDatos({ ...datos, nombre: e.target.value })}
                  onBlur={() => setTocado(true)}
                />
                {tocado && errores.nombre && <span className="error">{errores.nombre}</span>}
              </label>
              <label className="campo">
                <span>Celular</span>
                <input
                  className="entrada num"
                  inputMode="numeric"
                  maxLength={9}
                  autoComplete="tel-national"
                  value={datos.celular}
                  aria-invalid={tocado && !!errores.celular}
                  onChange={(e) => setDatos({ ...datos, celular: e.target.value.replace(/\D/g, '') })}
                  onBlur={() => setTocado(true)}
                />
                {tocado && errores.celular ? <span className="error">{errores.celular}</span> : <small>Te avisamos cuando esté listo.</small>}
              </label>
              {tipo === 'DELIVERY' && (
                <>
                  <label className="campo">
                    <span>Dirección</span>
                    <input
                      className="entrada"
                      autoComplete="street-address"
                      value={datos.direccion}
                      aria-invalid={tocado && !!errores.direccion}
                      onChange={(e) => setDatos({ ...datos, direccion: e.target.value })}
                    />
                    {tocado && errores.direccion && <span className="error">{errores.direccion}</span>}
                  </label>
                  <label className="campo">
                    <span>Referencia (opcional)</span>
                    <input
                      className="entrada"
                      value={datos.referencia}
                      onChange={(e) => setDatos({ ...datos, referencia: e.target.value })}
                      placeholder="Casa verde, frente al parque"
                    />
                  </label>
                </>
              )}
            </div>
          }
        />
      )}
    </>
  )
}
