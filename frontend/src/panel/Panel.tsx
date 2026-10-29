import { NavLink, Navigate, Route, Routes } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/cliente'
import type { Rol } from '../api/tipos'
import { useSesion } from '../lib/sesion'
import { useVivo } from '../lib/vivo'
import { Marca } from '../carta/Marca'
import { Login } from './Login'
import { Cocina } from './Cocina'
import { Mesas } from './Mesas'
import { Caja } from './Caja'
import { CartaAdmin } from './CartaAdmin'
import type { Pedido } from '../api/tipos'
import './panel.css'

const INICIO: Record<Rol, string> = {
  COCINA: '/panel/cocina',
  MOZO: '/panel/mesas',
  CAJA: '/panel/caja',
  ADMIN: '/panel/caja',
}

export default function Panel() {
  const { usuario, cargando } = useSesion()
  if (cargando) return <div className="panel-cargando" aria-busy="true" />
  if (!usuario) return <Login />
  return <PanelConSesion rol={usuario.rol} />
}

function PanelConSesion({ rol }: { rol: Rol }) {
  const { usuario, salir } = useSesion()
  // Un solo canal en vivo para todo el panel: cualquier cambio refresca lo que esté en pantalla.
  useVivo(['/topic/staff'], [['pedidos'], ['mesas'], ['resumen'], ['carta-admin'], ['carta'], ['cuenta']], true)
  const pedidos = useQuery({ queryKey: ['pedidos'], queryFn: () => api<Pedido[]>('/api/pedidos') })
  const porConfirmar = pedidos.data?.filter((p) => p.estado === 'ENVIADO').length ?? 0

  const enlaces = [
    { a: '/panel/cocina', texto: 'Cocina', roles: ['ADMIN', 'COCINA', 'MOZO'] },
    { a: '/panel/mesas', texto: 'Mesas', roles: ['ADMIN', 'MOZO', 'CAJA'], cuenta: porConfirmar },
    { a: '/panel/caja', texto: 'Caja', roles: ['ADMIN', 'CAJA'] },
    { a: '/panel/carta', texto: 'Carta', roles: ['ADMIN', 'COCINA', 'MOZO', 'CAJA'] },
  ].filter((e) => e.roles.includes(rol))

  return (
    <div className="panel">
      <header className="panel-barra">
        <div className="panel-barra-interior">
          <a href="/" className="panel-marca" title="Ver la carta pública">
            <Marca tam={30} />
            <span>La Sazón de Doña Meche</span>
          </a>
          <nav aria-label="Secciones">
            {enlaces.map((e) => (
              <NavLink key={e.a} to={e.a} className="panel-enlace">
                {e.texto}
                {e.cuenta ? <span className="panel-cuenta num" aria-label={`${e.cuenta} por confirmar`}>{e.cuenta}</span> : null}
              </NavLink>
            ))}
          </nav>
          <button className="btn btn-texto btn-chico panel-salir" onClick={salir} title="Cerrar sesión">
            <span className="panel-usuario">{usuario?.nombre}</span>
            <LogOut size={16} />
          </button>
        </div>
      </header>
      <Routes>
        <Route index element={<Navigate to={INICIO[rol]} replace />} />
        <Route path="cocina" element={<Cocina />} />
        <Route path="mesas" element={<Mesas />} />
        <Route path="caja" element={rol === 'ADMIN' || rol === 'CAJA' ? <Caja /> : <Navigate to={INICIO[rol]} />} />
        <Route path="carta" element={<CartaAdmin />} />
        <Route path="*" element={<Navigate to={INICIO[rol]} replace />} />
      </Routes>
    </div>
  )
}
