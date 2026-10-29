import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { CartaMesa } from './carta/CartaMesa'
import { CartaExterna } from './carta/CartaExterna'
import { Seguimiento } from './carta/Seguimiento'
import { AvisoDespertar } from './ui/AvisoDespertar'

// El panel del personal no se descarga en el celular del comensal.
const Panel = lazy(() => import('./panel/Panel'))

export default function App() {
  return (
    <>
      <AvisoDespertar />
      <Suspense fallback={null}>
        <Routes>
          <Route path="/" element={<CartaExterna />} />
          <Route path="/m/:token" element={<CartaMesa />} />
          <Route path="/pedido/:codigo" element={<Seguimiento />} />
          <Route path="/panel/*" element={<Panel />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </>
  )
}
