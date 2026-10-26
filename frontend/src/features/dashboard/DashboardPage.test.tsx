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
  deadlines: [
    { kind: 'MID_STAY_REPORT', resident: { id: 'p9', number: 9, name: 'Hugo', surname: 'Peña' }, dueDate: '2026-09-01',
      daysLeft: -50, overdue: true, reportId: 'r9', detail: null },
    { kind: 'COURT_MEASURE_END', resident: { id: 'p2', number: 2, name: 'Elena', surname: 'Mas' }, dueDate: '2026-10-25',
      daysLeft: 4, overdue: false, reportId: null, detail: 'Juzgado de lo Penal nº 3' },
  ],
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

    // Las filas llevan cada dato dos veces: en su columna y en el resumen del móvil (el CSS oculta uno)
    // Lo que vence: el informe enlaza al informe y la medida dice el juzgado
    expect(screen.getAllByText('Vencido hace 50 días')[0]).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Informe de media estancia' })[0]).toHaveAttribute('href', '/informes/r9')
    expect(screen.getByText('Juzgado de lo Penal nº 3')).toBeInTheDocument()
    expect(screen.getAllByText('En 4 días')[0]).toBeInTheDocument()

    // El desglose, también como tabla
    await userEvent.click(screen.getByText('Ver como tabla'))
    expect(screen.getByRole('columnheader', { name: 'Total' })).toBeVisible()
  })
})
