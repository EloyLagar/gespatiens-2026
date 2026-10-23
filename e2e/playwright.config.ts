import { defineConfig, devices } from '@playwright/test'

// Contra la app entera (docker compose up, perfil demo). E2E_BASE_URL cambia dónde está
export default defineConfig({
  testDir: './tests',
  // Las pruebas cambian datos (rellenan un turno, cierran un informe): una detrás de otra
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8080',
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    trace: 'retain-on-failure',
  },
  // E2E_CHANNEL=chrome usa el Chrome instalado en lugar del que descarga Playwright
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], channel: process.env.E2E_CHANNEL } }],
})
