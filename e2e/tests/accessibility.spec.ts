import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'
import { login } from './helpers'

// Accesibilidad automática en cada cambio: axe revisa cada pantalla con las reglas de WCAG 2.1 A y AA (y los
// tamaños mínimos de WCAG 2.2), en el tema claro, en el oscuro y en el móvil. Si una pantalla nueva trae un
// botón sin nombre o un color sin contraste, el CI sale en rojo. Lo que axe no ve (el orden del foco, cómo
// suena con un lector de pantalla) se revisa a mano: docs/accesibilidad-revision-manual.md
const WCAG = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']

const SCREENS = [
  '/avisos',
  '/diario',
  '/informes',
  '/valoraciones',
  '/turnos',
  '/intervenciones',
  '/residentes',
  '/antiguos',
  '/empleados',
  '/panel',
  '/registro',
  '/perfil',
  '/residentes/nuevo',
]

async function expectNoViolations(page: Page) {
  // Con los datos ya pintados: los esqueletos de carga no son la pantalla
  await page.waitForLoadState('networkidle')
  const { violations } = await new AxeBuilder({ page }).withTags(WCAG).analyze()
  // Solo lo necesario para arreglarlo: la regla y dónde está
  expect(violations.map((v) => ({ rule: v.id, where: v.nodes.map((n) => n.target.join(' ')).slice(0, 5) }))).toEqual([])
}

for (const colorScheme of ['light', 'dark'] as const) {
  test.describe(`tema ${colorScheme === 'light' ? 'claro' : 'oscuro'}`, () => {
    test.use({ colorScheme })

    test('el login y la declaración de accesibilidad, sin haber entrado', async ({ page }) => {
      for (const path of ['/login', '/accesibilidad']) {
        await page.goto(path)
        await expectNoViolations(page)
      }
    })

    test('todas las pantallas de la administración', async ({ page }) => {
      await login(page)
      for (const path of SCREENS) {
        await page.goto(path)
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
        await expectNoViolations(page)
      }
    })
  })
}

test.describe('móvil', () => {
  test.use({ viewport: { width: 375, height: 812 } })

  test('las pantallas principales y el menú abierto', async ({ page }) => {
    await login(page)
    for (const path of ['/avisos', '/diario', '/turnos', '/empleados', '/panel']) {
      await page.goto(path)
      await expectNoViolations(page)
    }
    await page.getByRole('button', { name: 'Abrir el menú' }).click()
    await expectNoViolations(page)
  })
})

// Quien activa "reducir movimiento" en el sistema (mareos, migrañas) no ve animaciones: app.scss las deja en
// casi cero con prefers-reduced-motion
test.describe('movimiento reducido', () => {
  async function submitTransition(page: Page) {
    await page.goto('/login')
    return page.getByRole('button', { name: 'Entrar', exact: true })
      .evaluate((button) => getComputedStyle(button).transitionDuration)
  }

  test('sin la preferencia, los botones tienen su transición', async ({ page }) => {
    expect(parseFloat(await submitTransition(page))).toBeGreaterThan(0.01)
  })

  test.describe(() => {
    test.use({ reducedMotion: 'reduce' })

    test('con la preferencia, las transiciones desaparecen', async ({ page }) => {
      expect(parseFloat(await submitTransition(page))).toBeLessThan(0.001)
    })
  })
})
