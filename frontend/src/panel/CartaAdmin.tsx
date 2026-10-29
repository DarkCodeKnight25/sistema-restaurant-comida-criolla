import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CircleAlert, Plus } from 'lucide-react'
import { api } from '../api/cliente'
import type { Carta, Plato } from '../api/tipos'
import { precioCartel } from '../lib/formato'
import { useSesion } from '../lib/sesion'
import { useTostada } from '../ui/Tostada'

/** La carta del día: marcar «se acabó» al instante (todos) y editar platos (administración). */
export function CartaAdmin() {
  const carta = useQuery({ queryKey: ['carta-admin'], queryFn: () => api<Carta>('/api/carta') })
  const { usuario } = useSesion()
  const admin = usuario?.rol === 'ADMIN'
  const [editando, setEditando] = useState<number | 'nuevo' | null>(null)

  if (carta.error) {
    return (
      <main className="pagina">
        <div className="error-bloque">
          <CircleAlert size={16} /> {carta.error.message}
        </div>
      </main>
    )
  }
  const c = carta.data
  const agotados = c?.platos.filter((p) => !p.disponible && p.activo).length ?? 0

  return (
    <main className="pagina pagina-angosta">
      <header className="fila-titulo">
        <div>
          <h1>Carta del día</h1>
          <p className="hallazgo">
            {agotados > 0 ? `${agotados} ${agotados === 1 ? 'plato agotado' : 'platos agotados'}. ` : 'Todo disponible. '}
            Lo que marques como agotado desaparece al instante del celular de los comensales.
          </p>
        </div>
        {admin && (
          <button className="btn btn-primario" onClick={() => setEditando('nuevo')}>
            <Plus size={16} /> Nuevo plato
          </button>
        )}
      </header>

      {admin && editando === 'nuevo' && c && <FormPlato carta={c} cerrar={() => setEditando(null)} />}

      {c?.categorias.map((cat) => {
        const platos = c.platos.filter((p) => p.categoriaId === cat.id)
        if (platos.length === 0) return null
        return (
          <section key={cat.id} className="admin-categoria" aria-labelledby={`adm-${cat.id}`}>
            <h2 id={`adm-${cat.id}`} className="titulo-bloque">
              {cat.nombre}
            </h2>
            <ul className="admin-platos">
              {platos.map((p) =>
                editando === p.id ? (
                  <li key={p.id}>
                    <FormPlato carta={c} plato={p} cerrar={() => setEditando(null)} />
                  </li>
                ) : (
                  <FilaPlato key={p.id} plato={p} carta={c} admin={admin} editar={() => setEditando(p.id)} />
                ),
              )}
            </ul>
          </section>
        )
      })}
    </main>
  )
}

function FilaPlato({ plato, carta, admin, editar }: { plato: Plato; carta: Carta; admin: boolean; editar: () => void }) {
  const cliente = useQueryClient()
  const tostada = useTostada()
  const estacion = carta.estaciones.find((e) => e.id === plato.estacionId)
  const cambiar = useMutation({
    mutationFn: (disponible: boolean) => api<Plato>(`/api/carta/platos/${plato.id}/disponible`, { metodo: 'PATCH', cuerpo: { disponible } }),
    onSuccess: (p) => {
      cliente.invalidateQueries({ queryKey: ['carta-admin'] })
      tostada({ texto: p.disponible ? `${p.nombre}: hay de nuevo.` : `${p.nombre}: marcado como agotado.` })
    },
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })
  return (
    <li className={'admin-plato' + (plato.activo ? '' : ' retirado')}>
      {plato.foto ? <img src={'/' + plato.foto} alt="" width={56} height={42} /> : <span className="admin-sin-foto" aria-hidden="true" />}
      <span className="admin-nombre">
        <strong>{plato.nombre}</strong>
        <span className="etiqueta etiqueta-chica" style={{ ['--color' as string]: estacion?.color }}>
          {estacion?.nombre}
        </span>
        {!plato.activo && <span className="estado">Retirado de la carta</span>}
        {plato.activo && !plato.disponible && <span className="cinta-chica rotulo">Se acabó</span>}
      </span>
      <span className="rotulo admin-precio">{precioCartel(plato.precio)}</span>
      <label className={'interruptor' + (plato.disponible ? '' : ' apagado')}>
        <input type="checkbox" checked={plato.disponible} disabled={cambiar.isPending || !plato.activo} onChange={(e) => cambiar.mutate(e.target.checked)} />
        <span>{plato.disponible ? 'Hay' : 'Se acabó'}</span>
      </label>
      {admin && (
        <button className="btn btn-texto btn-chico" onClick={editar}>
          Editar
        </button>
      )}
    </li>
  )
}

function FormPlato({ carta, plato, cerrar }: { carta: Carta; plato?: Plato; cerrar: () => void }) {
  const cliente = useQueryClient()
  const tostada = useTostada()
  const [f, setF] = useState({
    nombre: plato?.nombre ?? '',
    descripcion: plato?.descripcion ?? '',
    precio: plato ? String(plato.precio) : '',
    categoriaId: plato?.categoriaId ?? carta.categorias[0]?.id,
    estacionId: plato?.estacionId ?? carta.estaciones[0]?.id,
    foto: plato?.foto ?? '',
    activo: plato?.activo ?? true,
  })
  const guardar = useMutation({
    mutationFn: () =>
      api<Plato>(plato ? `/api/carta/platos/${plato.id}` : '/api/carta/platos', {
        metodo: plato ? 'PUT' : 'POST',
        cuerpo: { ...f, precio: Number(f.precio) },
      }),
    onSuccess: (p) => {
      cliente.invalidateQueries({ queryKey: ['carta-admin'] })
      tostada({ texto: `${p.nombre} guardado.` })
      cerrar()
    },
    onError: (e) => tostada({ texto: e.message, mal: true }),
  })
  return (
    <form
      className="form-plato"
      onSubmit={(e) => {
        e.preventDefault()
        guardar.mutate()
      }}
    >
      <label className="campo">
        <span>Nombre</span>
        <input className="entrada" required maxLength={80} value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })} />
      </label>
      <label className="campo">
        <span>Precio (S/)</span>
        <input className="entrada num" required inputMode="decimal" value={f.precio} onChange={(e) => setF({ ...f, precio: e.target.value.replace(/[^\d.]/g, '') })} />
      </label>
      <label className="campo form-ancho">
        <span>Descripción</span>
        <input className="entrada" maxLength={240} value={f.descripcion} onChange={(e) => setF({ ...f, descripcion: e.target.value })} />
      </label>
      <label className="campo">
        <span>Sección</span>
        <select className="entrada" value={f.categoriaId} onChange={(e) => setF({ ...f, categoriaId: Number(e.target.value) })}>
          {carta.categorias.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}
            </option>
          ))}
        </select>
      </label>
      <label className="campo">
        <span>Estación de cocina</span>
        <select className="entrada" value={f.estacionId} onChange={(e) => setF({ ...f, estacionId: Number(e.target.value) })}>
          {carta.estaciones.map((e) => (
            <option key={e.id} value={e.id}>
              {e.nombre}
            </option>
          ))}
        </select>
      </label>
      <label className="campo form-ancho">
        <span>Foto (ruta)</span>
        <input className="entrada" maxLength={120} value={f.foto} onChange={(e) => setF({ ...f, foto: e.target.value })} placeholder="platos/mi-plato.webp" />
      </label>
      <label className="casilla">
        <input type="checkbox" checked={f.activo} onChange={(e) => setF({ ...f, activo: e.target.checked })} /> Visible en la carta
      </label>
      <div className="form-acciones">
        <button type="button" className="btn btn-texto" onClick={cerrar}>
          Cancelar
        </button>
        <button className="btn btn-primario" disabled={guardar.isPending}>
          Guardar
        </button>
      </div>
    </form>
  )
}
