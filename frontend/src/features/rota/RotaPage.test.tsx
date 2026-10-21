import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { Rota, ShiftSwap } from '../../api/rota'
import { RotaPage } from './RotaPage'

const auth = vi.hoisted(() => ({
  isAdmin: false,
  user: { id: 'pablo', name: 'Pablo', speciality: 'SOCIAL_EDUCATOR' },
}))
vi.mock('../../auth/AuthContext', () => ({ useAuth: () => auth }))

const pablo = { id: 'pablo', name: 'Pablo', speciality: 'SOCIAL_EDUCATOR' as const }
const ana = { id: 'ana', name: 'Ana', speciality: 'PSYCHOLOGIST' as const }

function rota(): Rota {
  return {
    month: '2026-10',
    employees: [pablo, ana],
    shifts: [
      { id: 's1', date: '2026-10-25', part: 'MORNING', employeeId: 'pablo' },
      { id: 's2', date: '2026-10-27', part: 'NIGHT', employeeId: 'ana' },
      { id: 's0', date: '2026-10-02', part: 'MORNING', employeeId: 'pablo' },
    ],
  }
}

function incoming(): ShiftSwap {
  return {
    id: 'w1', status: 'PENDING', requester: ana, partner: pablo,
    requesterShift: { id: 's2', date: '2026-10-27', part: 'NIGHT', employeeId: 'ana' },
    partnerShift: null, createdAt: '2026-10-20T10:00:00Z', resolvedAt: null,
  }
}

function json(body: unknown, status = 200) {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status, headers: { 'Content-Type': 'application/json' },
  })
}

function api(swaps: ShiftSwap[] = []) {
  return vi.fn().mockImplementation(async (url: string, init?: RequestInit) => {
    if (url.startsWith('/api/rota?month=')) return json(rota())
    if (url === '/api/rota/swaps' && init?.method === 'POST') return json({ ...incoming(), id: 'w2' }, 201)
    if (url === '/api/rota/swaps') return json(swaps)
    if (url === '/api/rota/shifts' && init?.method === 'PUT') {
      return json({ id: 'new', ...JSON.parse(String(init.body)) })
    }
    if (url.startsWith('/api/rota/swaps/')) return json({ ...incoming(), status: 'ACCEPTED' })
    return json(undefined, 204)
  })
}

describe('RotaPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 21, 10))
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
    auth.isAdmin = false
  })

  it('cada trabajador ve sus turnos y puede pedir un cambio de los que aún no han llegado', async () => {
    const fetchMock = api()
    vi.stubGlobal('fetch', fetchMock)
    render(<RotaPage />)
    const user = userEvent.setup()

    expect(await screen.findByText('Tienes 2 turnos planificados este mes')).toBeInTheDocument()
    // El del día 2 ya pasó: no se puede cambiar
    expect(screen.getAllByRole('button', { name: /Pedir un cambio/ })).toHaveLength(1)

    await user.click(screen.getByRole('button', { name: 'Pedir un cambio del turno de Mañana del 25/10/2026' }))
    await user.selectOptions(screen.getByLabelText('Compañero o compañera'), 'ana')
    await user.selectOptions(screen.getByLabelText('Su turno a cambio'), 's2')
    await user.click(screen.getByRole('button', { name: 'Enviar la petición' }))

    await waitFor(() => {
      const post = fetchMock.mock.calls.find(([url, init]) => url === '/api/rota/swaps' && init?.method === 'POST')
      expect(JSON.parse(String(post?.[1]?.body))).toEqual({ myShiftId: 's1', partnerId: 'ana', partnerShiftId: 's2' })
    })
  })

  it('un cambio que le piden se acepta sin pasar por la administración', async () => {
    const fetchMock = api([incoming()])
    vi.stubGlobal('fetch', fetchMock)
    render(<RotaPage />)
    const user = userEvent.setup()

    const item = (await screen.findByText(/Ana te pide que cubras su noche del 27\/10\/2026/)).closest('li')!
    await user.click(within(item).getByRole('button', { name: 'Aceptar' }))

    expect(await screen.findByText('Cambio aceptado: el cuadrante ya está actualizado')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/rota/swaps/w1/accept', expect.objectContaining({ method: 'POST' }))
  })

  it('el resto de la plantilla consulta el cuadrante sin poder cambiarlo', async () => {
    vi.stubGlobal('fetch', api())
    render(<RotaPage />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Cuadrante' }))

    expect(screen.getByRole('table', { name: 'Cuadrante de turnos de octubre de 2026' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /de Ana el día/ })).not.toBeInTheDocument()
    expect(screen.queryByText('Herramientas de planificación')).not.toBeInTheDocument()
  })

  it('la administración pone un turno pulsando la casilla', async () => {
    auth.isAdmin = true
    const fetchMock = api()
    vi.stubGlobal('fetch', fetchMock)
    render(<RotaPage />)
    const user = userEvent.setup()

    const cell = await screen.findByRole('button', { name: 'Tarde de Ana el día 21' })
    expect(cell).toHaveAttribute('aria-pressed', 'false')
    await user.click(cell)

    await waitFor(() => expect(cell).toHaveAttribute('aria-pressed', 'true'))
    const put = fetchMock.mock.calls.find(([url, init]) => url === '/api/rota/shifts' && init?.method === 'PUT')
    expect(JSON.parse(String(put?.[1]?.body))).toEqual({ date: '2026-10-21', part: 'AFTERNOON', employeeId: 'ana' })
    expect(screen.getByText('Herramientas de planificación')).toBeInTheDocument()
  })
})
