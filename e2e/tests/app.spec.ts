import { expect, test } from '@playwright/test'
import { login } from './helpers'

// Los recorridos principales de la app, de punta a punta: pantalla, API y base de datos

test('se entra y la portada son los avisos del personal', async ({ page }) => {
  await login(page)
  await expect(page.getByText('El grupo terapéutico del viernes pasa a las 17:00.')).toBeVisible()
})

test('el turno de tarde de hoy se rellena desde el diario', async ({ page }) => {
  await login(page)
  await page.getByRole('link', { name: 'Diario' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Diario' })).toBeVisible()
  // La demostración deja la tarde y la noche de hoy sin rellenar: la primera es la tarde
  await page.getByRole('button', { name: 'Rellenar' }).first().click()
  await page.getByLabel('Información de interés').fill('Prueba de extremo a extremo: tarde sin incidencias.')
  await page.getByRole('button', { name: 'Guardar' }).click()
  await expect(page.getByText('Prueba de extremo a extremo: tarde sin incidencias.')).toBeVisible()
  await expect(page.getByText(/Lo rellenó Marta Serra/)).toBeVisible()
})

test('un informe completo se descarga en PDF y se cierra', async ({ page }) => {
  await login(page)
  await page.goto('/informes?q=Lucía&tipo=MID_STAY')
  await page.getByRole('link', { name: 'Lucía Martínez Soler' }).click()
  await expect(page.getByText('28 de 28 apartados redactados')).toBeVisible()

  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: /Descargar PDF/ }).click()
  expect((await download).suggestedFilename()).toMatch(/\.pdf$/)

  await page.getByRole('button', { name: 'Cerrar informe' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Cerrar informe' }).click()
  await expect(page.getByText(/Informe cerrado/)).toBeVisible()
})

test('el panel avisa de los vencimientos cercanos', async ({ page }) => {
  await login(page)
  await page.getByRole('link', { name: 'Panel de control' }).click()
  const deadlines = page.getByRole('table', { name: 'Vencimientos' })
  await expect(deadlines).toBeVisible()
  await expect(deadlines.getByText('Alejandro Ruiz Peña')).toBeVisible()
  await expect(deadlines.getByText('Fin de la medida judicial').first()).toBeVisible()
})

test('las valoraciones del mes muestran las notas de la demostración', async ({ page }) => {
  await login(page)
  await page.getByRole('link', { name: 'Valoraciones' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Valoraciones' })).toBeVisible()
  // El taller ocupacional es de lunes a viernes: el mes en curso siempre tiene alguno (salvo su primer fin de semana)
  await page.getByLabel('Actividad').selectOption({ label: 'Taller ocupacional' })
  await expect(page.getByText(/Lucía/).first()).toBeVisible()
})
