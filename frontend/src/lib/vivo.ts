import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { leerToken } from '../api/cliente'

// En producción la API vive en otro dominio (Render); en desarrollo, Vite hace de proxy de /ws.
function urlWs(): string {
  const api = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '')
  if (api) return api.replace(/^http/, 'ws') + '/ws'
  return `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`
}

/**
 * Escucha canales en vivo y, ante cualquier aviso, vuelve a pedir los datos indicados.
 * El servidor avisa después de guardar, así que el nuevo pedido siempre trae el cambio.
 */
export function useVivo(canales: string[], claves: unknown[][], conSesion = false) {
  const cliente = useQueryClient()
  const firma = canales.join('|')

  useEffect(() => {
    if (canales.length === 0) return
    const token = conSesion ? leerToken() : null
    const stomp = new Client({
      brokerURL: urlWs(),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 3000,
      heartbeatIncoming: 20000,
      heartbeatOutgoing: 20000,
    })
    stomp.onConnect = () => {
      canales.forEach((c) =>
        stomp.subscribe(c, () => {
          claves.forEach((k) => cliente.invalidateQueries({ queryKey: k }))
        }),
      )
      // Al reconectar puede haberse perdido algún aviso: se refresca todo una vez.
      claves.forEach((k) => cliente.invalidateQueries({ queryKey: k }))
    }
    stomp.activate()
    return () => {
      stomp.deactivate()
    }
  }, [firma, conSesion]) // eslint-disable-line react-hooks/exhaustive-deps
}
