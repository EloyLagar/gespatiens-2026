import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { Dashboard } from '../../api/dashboard'
import { DashboardPage } from './DashboardPage'

const DATA: Dashboard = {
  month: '2026-10', occupiedPlaces: 27, totalPlaces: 32, residentsWithoutBothTutors: 3, reportsInProgress: 12,
  interventions: 41,
  shifts: [
    { employeeId: 'e1', name: 'Pablo', speciality: 'SOCIAL_EDUCATOR', morning: 4, afternoon: 2, night: 3, total: 9 },
    { employeeId: 'e2', name: 'Ana', speciality: 'SOCIAL_EDUCATOR', morning: 2, afternoon: 1, night: 0, total: 3 },
  ],
  noticesByWeek: [{ monday: '2026-09-28', notices: 3 }, { monday: '2026-10-05', notices: 7 }],
}

describe('DashboardPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  // Memoria final, 5.1: turnos por trabajador en el mes y avisos por semana, más las cifras de hoy
  it('muestra las cifras, los turnos por trabajador y los avisos por semana', async () => {
    const fetchMock = vi.fn().mockImplementation(async () =>
      new Response(JSON.stringify(DATA), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)
    render(<MemoryRouter><DashboardPage /></MemoryRouter>)

    expect(await screen.findByText('27')).toBeInTheDocument()
    expect(screen.getByText('Quedan 5 plazas libres')).toBeInTheDocument()
    expect(screen.getByRole('meter', { name: 'Plazas ocupadas' })).toHaveAttribute('aria-valuenow', '27')
    expect(screen.getByLabelText('Pablo: 9 · Mañana 4 · tarde 2 · noche 3')).toBeInTheDocument()
    expect(screen.getByLabelText(/Semana del .*: 7 avisos/)).toBeInTheDocument()
    expect(String(fetchMock.mock.calls[0][0])).toMatch(/^\/api\/dashboard\?month=\d{4}-\d{2}$/)

    // El desglose, también como tabla
    await userEvent.click(screen.getByText('Ver como tabla'))
    expect(screen.getByRole('columnheader', { name: 'Total' })).toBeVisible()
  })
})
