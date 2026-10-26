import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useEffect } from 'react'
import { MemoryRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import i18n from '../../i18n'
import { SPECIALITIES, type Speciality } from '../../api/types'
import { AuthProvider } from '../../auth/AuthContext'
import { TUTORIAL_STEPS } from './steps'
import { TutorialProvider, useTutorial } from './Tutorial'

const markMyTutorialSeen = vi.fn(() => Promise.resolve())
vi.mock('../../api/users', () => ({ markMyTutorialSeen: () => markMyTutorialSeen() }))

function session(speciality: Speciality, tutorialSeen = false) {
  return {
    token: 'token',
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    user: { id: `u-${speciality}`, name: 'Ana Beltrán', email: 'ana@vientos.es', phoneNumber: '600111222', speciality, language: 'ES', tutorialSeen },
  }
}

// La pantalla de detrás: solo dice dónde está y deja repetir el tutorial, como Mi perfil
function Screen() {
  const location = useLocation()
  const tutorial = useTutorial()
  return (
    <>
      <p>Pantalla {location.pathname}</p>
      <button type="button" onClick={tutorial.start}>Repetir</button>
    </>
  )
}

// Como el diario de verdad: /diario lleva al día de hoy
let diaryRedirects = 0
function DiaryRedirect() {
  useEffect(() => {
    diaryRedirects++
  }, [])
  return <Navigate to="/diario/2026-10-26" replace />
}

// Lo que la sesión dice ahora del tutorial (la aplicación lo lee de ahí al volver a entrar)
function sessionSaysSeen(): boolean {
  return JSON.parse(sessionStorage.getItem('gespatiens.session') ?? '{}').user?.tutorialSeen === true
}

function renderApp(speciality: Speciality, url = '/avisos', tutorialSeen = false) {
  sessionStorage.setItem('gespatiens.session', JSON.stringify(session(speciality, tutorialSeen)))
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[url]}>
        <TutorialProvider>
          <Routes>
            <Route path="diario" element={<DiaryRedirect />} />
            <Route path="*" element={<Screen />} />
          </Routes>
        </TutorialProvider>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('Tutorial de bienvenida', () => {
  afterEach(() => {
    sessionStorage.clear()
    markMyTutorialSeen.mockClear()
  })

  it('la primera vez que entra un educador le enseña sus apartados, abriendo cada uno', async () => {
    renderApp('SOCIAL_EDUCATOR')

    expect(screen.getByRole('dialog', { name: 'Hola, Ana' })).toBeInTheDocument()
    expect(screen.getByText('Paso 1 de 10')).toBeInTheDocument()
    expect(screen.getByText(/lo que más vas a usar es el diario/)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Empezar' }))
    expect(screen.getByRole('dialog', { name: 'Avisos' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(screen.getByText('Pantalla /diario/2026-10-26')).toBeInTheDocument()
    expect(screen.getByText(/pones la nota de cada residente/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(screen.getByText('Pantalla /valoraciones')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Anterior' }))
    expect(screen.getByRole('dialog', { name: 'Diario' })).toBeInTheDocument()
  })

  // Regresión: el tutorial navegaba en un efecto que dependía de navigate. Tras la redirección de /diario
  // el efecto volvía a mandar a /diario, y así sin parar: el diario se montaba una y otra vez
  it('abre cada apartado una sola vez aunque el apartado redirija', async () => {
    diaryRedirects = 0
    renderApp('SOCIAL_EDUCATOR')
    await userEvent.click(screen.getByRole('button', { name: 'Empezar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    await act(() => new Promise((resolve) => setTimeout(resolve, 50)))
    expect(screen.getByText('Pantalla /diario/2026-10-26')).toBeInTheDocument()
    expect(diaryRedirects).toBe(1)
  })

  it('cada especialidad tiene el suyo', () => {
    renderApp('MEDICAL_TEAM')
    expect(screen.getByText(/registras las rebajas en el diario/)).toBeInTheDocument()
    expect(screen.getByText('Paso 1 de 8')).toBeInTheDocument()
  })

  it('al acabarlo deja al empleado en los avisos y no vuelve a salir', async () => {
    const { unmount } = renderApp('MEDICAL_TEAM', '/perfil')
    for (let i = 0; i < 7; i++) await userEvent.click(screen.getByRole('button', { name: /Empezar$|Siguiente/ }))
    expect(screen.getByRole('dialog', { name: 'Ya está' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Saltar el tutorial' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Empezar a trabajar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByText('Pantalla /avisos')).toBeInTheDocument()
    expect(markMyTutorialSeen).toHaveBeenCalledTimes(1)
    expect(sessionSaysSeen()).toBe(true)

    unmount()
    renderApp('MEDICAL_TEAM', '/avisos', true)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('saltarlo, también con Escape, vuelve a la pantalla de la que se partió', async () => {
    renderApp('PSYCHOLOGIST', '/residentes')
    await userEvent.click(screen.getByRole('button', { name: 'Empezar' }))
    expect(screen.getByText('Pantalla /avisos')).toBeInTheDocument()

    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByText('Pantalla /residentes')).toBeInTheDocument()
    expect(markMyTutorialSeen).toHaveBeenCalledTimes(1)
    expect(sessionSaysSeen()).toBe(true)
  })

  it('se repite desde la aplicación o con ?tutorial aunque ya se haya visto', async () => {
    const { unmount } = renderApp('SOCIAL_WORKER', '/avisos', true)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Repetir' }))
    expect(screen.getByRole('dialog', { name: 'Hola, Ana' })).toBeInTheDocument()

    unmount()
    renderApp('SOCIAL_WORKER', '/turnos?tutorial', true)
    expect(screen.getByRole('dialog', { name: 'Hola, Ana' })).toBeInTheDocument()
    // Ya constaba como visto: repetirlo no vuelve a llamar a la API
    await userEvent.keyboard('{Escape}')
    expect(markMyTutorialSeen).not.toHaveBeenCalled()
  })

  it('el foco va al título de cada paso', async () => {
    renderApp('ADMINISTRATOR')
    expect(screen.getByRole('heading', { name: 'Hola, Ana' })).toHaveFocus()
    await userEvent.click(screen.getByRole('button', { name: 'Empezar' }))
    expect(screen.getByRole('heading', { name: 'Panel de control' })).toHaveFocus()
  })
})

describe('Pasos del tutorial', () => {
  // El panel y el registro llevan RequireAdmin: el tutorial no puede mandar ahí a nadie más
  it('solo el de la administración abre apartados de la administración', () => {
    for (const speciality of SPECIALITIES) {
      const routes = TUTORIAL_STEPS[speciality].map((step) => step.to)
      const adminOnly = routes.some((to) => to === '/panel' || to === '/registro')
      expect(adminOnly, speciality).toBe(speciality === 'ADMINISTRATOR')
    }
  })

  it('todos los pasos tienen título y texto en los dos idiomas', () => {
    for (const language of ['es', 'en']) {
      for (const speciality of SPECIALITIES) {
        for (const { key } of TUTORIAL_STEPS[speciality]) {
          for (const part of ['title', 'text']) {
            const found = [`tutorial.${speciality}.${key}.${part}`, `tutorial.common.${key}.${part}`]
              .some((k) => i18n.exists(k, { lng: language }))
            expect(found, `${language} ${speciality} ${key}.${part}`).toBe(true)
          }
        }
      }
    }
  })
})
