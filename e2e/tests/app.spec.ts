import { expect, test } from '@playwright/test'
import { ADMIN, login } from './helpers'

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
  // Sale en la cabecera y en la tabla de apartados
  await expect(page.getByText('28 de 28 apartados redactados').first()).toBeVisible()

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
  // Tiene más de un vencimiento, así que sale en varias filas
  await expect(deadlines.getByText('Alejandro Ruiz Peña').first()).toBeVisible()
  // El qué también va repetido en la línea de debajo del nombre, que solo se ve en el móvil: se busca el visible
  await expect(deadlines.getByText('Fin de la medida judicial').filter({ visible: true }).first()).toBeVisible()
})

test('las valoraciones del mes muestran las notas de la demostración', async ({ page }) => {
  await login(page)
  await page.getByRole('link', { name: 'Valoraciones' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Valoraciones' })).toBeVisible()
  // El taller ocupacional es de lunes a viernes: el mes en curso siempre tiene alguno (salvo su primer fin de semana)
  await page.getByLabel('Actividad').selectOption({ label: 'Taller ocupacional' })
  await expect(page.getByText(/Lucía/).first()).toBeVisible()
})

// Laura solo entra en esta prueba: con la base de datos recién cargada (como en CI), todavía no lo ha visto
test('el tutorial de bienvenida es el de la especialidad y no vuelve a salir', async ({ page }) => {
  await login(page, { email: 'laura.gil@vientos.es', password: ADMIN.password }, { keepTutorial: true })
  await expect(page.getByRole('dialog', { name: 'Hola, Laura' })).toBeVisible()
  await expect(page.getByText(/registras las rebajas en el diario/)).toBeVisible()
  await page.getByRole('button', { name: 'Empezar' }).click()
  await page.getByRole('button', { name: 'Siguiente' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Diario' })).toBeVisible()
  await page.getByRole('button', { name: 'Saltar el tutorial' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)

  // Otra pestaña es otra sesión, como entrar desde otro equipo: queda guardado en su perfil
  const other = await page.context().newPage()
  await login(other, { email: 'laura.gil@vientos.es', password: ADMIN.password }, { keepTutorial: true })
  await expect(other.getByRole('dialog')).toHaveCount(0)
})
