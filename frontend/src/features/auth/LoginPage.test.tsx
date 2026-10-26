import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import { LoginPage } from './LoginPage'

const auth = vi.hoisted(() => ({ user: null, endedBecause: null, login: vi.fn() }))
vi.mock('../../auth/AuthContext', () => ({ useAuth: () => auth }))

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function renderLogin() {
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )
}

describe('LoginPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.useRealTimers()
    auth.login.mockReset()
  })

  it('en la demo entra con un clic con la cuenta del perfil elegido', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json([
      { name: 'Marta Serra', email: 'marta.serra@vientos.es', speciality: 'ADMINISTRATOR', password: 'demo1234' },
      { name: 'Laura Gil', email: 'laura.gil@vientos.es', speciality: 'MEDICAL_TEAM', password: 'demo1234' },
    ])))
    renderLogin()

    expect(await screen.findByRole('heading', { name: 'Probar la demo' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Entrar como Equipo médico/ }))

    expect(auth.login).toHaveBeenCalledWith('laura.gil@vientos.es', 'demo1234')
  })

  it('fuera de la demo no enseña cuentas, solo el formulario', async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ detail: '' }, 401))
    vi.stubGlobal('fetch', fetchMock)
    renderLogin()

    await act(async () => {})
    expect(fetchMock).toHaveBeenCalledWith('/api/demo/accounts', expect.anything())
    expect(screen.queryByRole('heading', { name: 'Probar la demo' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toBeInTheDocument()
  })

  it('avisa de que el servidor está despertando si tarda en responder', async () => {
    vi.useFakeTimers()
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    renderLogin()

    expect(screen.queryByText(/Conectando con el servidor/)).not.toBeInTheDocument()
    await act(async () => {
      vi.advanceTimersByTime(3000)
    })
    expect(screen.getByText(/puede tardar hasta un minuto/)).toBeInTheDocument()
  })
})
