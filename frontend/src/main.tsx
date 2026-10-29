import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import './estilos/ui.css'
import App from './App.tsx'
import { ProveedorSesion } from './lib/sesion.tsx'
import { ProveedorTostada } from './ui/Tostada.tsx'
import { ErrorApi } from './api/cliente.ts'

const cliente = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (intentos, error) => !(error instanceof ErrorApi && error.estado >= 400 && error.estado < 500) && intentos < 2,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={cliente}>
      <BrowserRouter>
        <ProveedorSesion>
          <ProveedorTostada>
            <App />
          </ProveedorTostada>
        </ProveedorSesion>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
