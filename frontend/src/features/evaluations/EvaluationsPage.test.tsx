import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { MonthlyEvaluations } from '../../api/diary'
import { EvaluationsPage } from './EvaluationsPage'

const auth = vi.hoisted(() => ({ isAdmin: false, user: { speciality: 'SOCIAL_EDUCATOR' } }))
vi.mock('../../auth/AuthContext', () => ({ useAuth: () => auth }))

// Una semana corta para el test: del jueves 1 al domingo 4 de octubre de 2026
function grid(): MonthlyEvaluations {
  const days = ['2026-10-01', '2026-10-02', '2026-10-03', '2026-10-04'].map((date, i) => ({
    date,
    activities: i === 1 ? [] : [{ id: `a${i}`, startTime: '10:00:00', title: null, version: 3 }],
  }))
  return {
    month: '2026-10',
    type: 'LIFE_SKILLS',
    days,
    rows: [{
      resident: { id: 'p1', number: 3, name: 'Juan', surname: 'García' },
      cells: [
        { mark: 8, attended: true, activityId: 'a0' },
        { mark: null, attended: false, activityId: null },
        { mark: 6, attended: true, activityId: 'a2' },
        { mark: null, attended: true, activityId: 'a3' },
      ],
    }],
  }
}

function api(saved = { version: 4 }) {
  return vi.fn().mockImplementation(async (_url: string, init?: RequestInit) =>
    new Response(JSON.stringify(init?.method === 'PUT' ? saved : grid()), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }))
}

describe('EvaluationsPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    auth.user = { speciality: 'SOCIAL_EDUCATOR' }
  })

  it('muestra las notas con la media de la semana y la del mes', async () => {
    vi.stubGlobal('fetch', api())
    render(<EvaluationsPage />)

    expect(await screen.findByLabelText('Nota de Juan García el día 1')).toHaveValue('8')
    expect(screen.getAllByText('7')).toHaveLength(2) // media de la semana 1 y del mes
    expect(screen.getByText('S1')).toBeInTheDocument()
  })

  it('una nota se guarda al salir de la casilla con la versión de la actividad', async () => {
    const fetchMock = api()
    vi.stubGlobal('fetch', fetchMock)
    render(<EvaluationsPage />)
    const user = userEvent.setup()

    const cell = await screen.findByLabelText('Nota de Juan García el día 4')
    await user.type(cell, '9,5')
    await user.tab()

    await waitFor(() => expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(true))
    const [url, init] = fetchMock.mock.calls.find(([, i]) => i?.method === 'PUT')!
    expect(url).toBe('/api/diary/activities/a3/evaluations/p1')
    expect(JSON.parse(init.body)).toEqual({ mark: 9.5, version: 3 })
    // Con la nota nueva, las medias se recalculan
    expect(await screen.findAllByText('7,8')).toHaveLength(2)
  })

  it('quien no valora solo ve las notas', async () => {
    auth.user = { speciality: 'PSYCHOLOGIST' }
    vi.stubGlobal('fetch', api())
    render(<EvaluationsPage />)

    expect(await screen.findByText('8')).toBeInTheDocument()
    expect(screen.queryByRole('textbox', { name: /Nota de/ })).not.toBeInTheDocument()
  })
})
