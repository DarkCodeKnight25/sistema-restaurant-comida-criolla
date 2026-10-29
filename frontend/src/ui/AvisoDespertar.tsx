import { useEffect, useState } from 'react'
import { escucharDespertar } from '../api/cliente'

/** Aviso honesto cuando el plan gratuito de Render está arrancando el servidor. */
export function AvisoDespertar() {
  const [visible, setVisible] = useState(false)
  useEffect(() => escucharDespertar(setVisible), [])
  if (!visible) return null
  return (
    <div className="aviso-despertar" role="status">
      <span className="punto-latido" aria-hidden="true" />
      Despertando el servidor de la demo. La primera carga puede tardar unos 30 segundos.
    </div>
  )
}
