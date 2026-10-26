/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // En desarrollo el navegador solo habla con Vite: las llamadas a /api se reenvían a Spring Boot.
    // Para el navegador todo es el mismo origen, así que no hace falta configurar CORS en la API.
    // API_URL cambia a qué API se reenvían, p. ej. la de la demo en línea para probar solo el frontend
    proxy: {
      '/api': {
        target: process.env.API_URL ?? 'http://localhost:8080',
        changeOrigin: true,
        // Sin el Origin de Vite, la API lo trata como una petición del mismo origen
        configure: (proxy) => proxy.on('proxyReq', (request) => request.removeHeader('origin')),
      },
    },
  },
  css: {
    preprocessorOptions: {
      // Carbon todavía usa funciones de Sass que la versión actual marca como obsoletas
      scss: { quietDeps: true, silenceDeprecations: ['import', 'global-builtin', 'if-function'] },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    // npx vitest run --coverage: el informe en coverage/ (el CI lo publica y saca el porcentaje del resumen)
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/main.tsx'],
      reporter: ['text-summary', 'json-summary', 'html'],
    },
  },
})
