import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'
import { CircleAlert, CircleCheck } from 'lucide-react'

interface Mensaje {
  texto: string
  mal?: boolean
}

const Contexto = createContext<(m: Mensaje) => void>(() => {})

export function ProveedorTostada({ children }: { children: ReactNode }) {
  const [mensaje, setMensaje] = useState<Mensaje | null>(null)
  const temporizador = useRef<number | undefined>(undefined)

  const mostrar = useCallback((m: Mensaje) => {
    setMensaje(m)
    window.clearTimeout(temporizador.current)
    temporizador.current = window.setTimeout(() => setMensaje(null), m.mal ? 7000 : 4000)
  }, [])

  return (
    <Contexto.Provider value={mostrar}>
      {children}
      <div aria-live="polite" role="status">
        {mensaje && (
          <div className={'tostada' + (mensaje.mal ? ' mal' : '')}>
            {mensaje.mal ? <CircleAlert size={16} /> : <CircleCheck size={16} />}
            {mensaje.texto}
          </div>
        )}
      </div>
    </Contexto.Provider>
  )
}

export function useTostada() {
  return useContext(Contexto)
}
