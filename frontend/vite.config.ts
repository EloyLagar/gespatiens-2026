/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // En desarrollo el navegador solo habla con Vite: las llamadas a /api se reenvían a Spring Boot.
    // Para el navegador todo es el mismo origen, así que no hace falta configurar CORS en la API
    proxy: {
      '/api': 'http://localhost:8080',
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
  },
})
