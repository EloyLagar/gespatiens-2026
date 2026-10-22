import { expect, type Page } from '@playwright/test'

// La administradora de la demostración (DemoData): ve todos los apartados, también el panel
// E2E_EMAIL y E2E_PASSWORD cambian el usuario (para probar contra otra base de datos)
export const ADMIN = {
  email: process.env.E2E_EMAIL ?? 'marta.serra@vientos.es',
  password: process.env.E2E_PASSWORD ?? 'demo1234',
}

export async function login(page: Page, user = ADMIN) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(user.email)
  await page.getByLabel('Contraseña', { exact: true }).fill(user.password)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Avisos' })).toBeVisible()
}
