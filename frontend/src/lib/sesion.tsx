import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { alExpirarSesion, api, guardarToken, leerToken } from '../api/cliente'
import type { Usuario } from '../api/tipos'

interface Sesion {
  usuario: Usuario | null
  cargando: boolean
  entrar: (email: string, password: string) => Promise<void>
  salir: () => void
}

const Contexto = createContext<Sesion | null>(null)

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [cargando, setCargando] = useState(() => leerToken() !== null)
  const cliente = useQueryClient()

  const salir = useCallback(() => {
    guardarToken(null)
    setUsuario(null)
    cliente.clear()
  }, [cliente])

  useEffect(() => {
    alExpirarSesion(() => setUsuario(null))
    if (!leerToken()) return
    api<Usuario>('/api/auth/yo')
      .then(setUsuario)
      .catch(() => guardarToken(null))
      .finally(() => setCargando(false))
  }, [])

  const entrar = useCallback(async (email: string, password: string) => {
    const r = await api<{ token: string; usuario: Usuario }>('/api/auth/login', {
      metodo: 'POST',
      cuerpo: { email, password },
    })
    guardarToken(r.token)
    setUsuario(r.usuario)
  }, [])

  const valor = useMemo(() => ({ usuario, cargando, entrar, salir }), [usuario, cargando, entrar, salir])
  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

export function useSesion() {
  const s = useContext(Contexto)
  if (!s) throw new Error('useSesion fuera de ProveedorSesion')
  return s
}

export function esCaja(u: Usuario | null) {
  return u?.rol === 'ADMIN' || u?.rol === 'CAJA'
}
