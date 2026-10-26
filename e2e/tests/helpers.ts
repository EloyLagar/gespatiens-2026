import { expect, type Page } from '@playwright/test'

// La administradora de la demostración (DemoData): ve todos los apartados, también el panel
// E2E_EMAIL y E2E_PASSWORD cambian el usuario (para probar contra otra base de datos)
export const ADMIN = {
  email: process.env.E2E_EMAIL ?? 'marta.serra@vientos.es',
  password: process.env.E2E_PASSWORD ?? 'demo1234',
}

// El tutorial de bienvenida sale la primera vez que entra cada empleado (se guarda en su perfil): si sale,
// se salta, salvo que la prueba sea la del tutorial. Sale junto con los avisos, así que basta mirar una vez
export async function login(page: Page, user = ADMIN, { keepTutorial = false } = {}) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(user.email)
  await page.getByLabel('Contraseña', { exact: true }).fill(user.password)
  // exact: en la demo también están los botones "Entrar como …" de cada perfil
  await page.getByRole('button', { name: 'Entrar', exact: true }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Avisos' })).toBeVisible()
  const skip = page.getByRole('button', { name: 'Saltar el tutorial' })
  if (!keepTutorial && (await skip.isVisible())) await skip.click()
}
