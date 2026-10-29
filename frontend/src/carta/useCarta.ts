import { useCallback, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/cliente'
import type { Carta, LineaCarrito, Local, Plato } from '../api/tipos'

export function useLocal() {
  return useQuery({ queryKey: ['local'], queryFn: () => api<Local>('/api/publico/local'), staleTime: 600_000 })
}

export function useCartaPublica() {
  return useQuery({ queryKey: ['carta'], queryFn: () => api<Carta>('/api/publico/carta') })
}

/** El pedido que se arma en el celular. Los precios se muestran de la carta; el servidor los vuelve a calcular. */
export function useCarrito(platos: Plato[] | undefined) {
  const [lineas, setLineas] = useState<LineaCarrito[]>([])

  const poner = useCallback((platoId: number, cantidad: number, nota = '') => {
    setLineas((ls) => {
      const resto = ls.filter((l) => l.platoId !== platoId)
      return cantidad <= 0 ? resto : [...resto, { platoId, cantidad: Math.min(cantidad, 30), nota }]
    })
  }, [])

  const quitar = useCallback((ids: number[]) => setLineas((ls) => ls.filter((l) => !ids.includes(l.platoId))), [])
  const vaciar = useCallback(() => setLineas([]), [])

  const detalle = useMemo(
    () =>
      lineas
        .map((l) => ({ ...l, plato: platos?.find((p) => p.id === l.platoId) }))
        .filter((l): l is LineaCarrito & { plato: Plato } => !!l.plato),
    [lineas, platos],
  )
  const total = detalle.reduce((s, l) => s + l.plato.precio * l.cantidad, 0)
  const cantidad = detalle.reduce((s, l) => s + l.cantidad, 0)
  const agotados = detalle.filter((l) => !l.plato.disponible).map((l) => l.platoId)

  return { lineas, detalle, total, cantidad, agotados, poner, quitar, vaciar }
}

export type Carrito = ReturnType<typeof useCarrito>
