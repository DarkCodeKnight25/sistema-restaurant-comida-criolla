import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5175,
    // En desarrollo la API de Spring Boot corre en el puerto 8083 (REST y WebSocket).
    proxy: {
      '/api': 'http://localhost:8083',
      '/ws': { target: 'ws://localhost:8083', ws: true },
    },
  },
})
