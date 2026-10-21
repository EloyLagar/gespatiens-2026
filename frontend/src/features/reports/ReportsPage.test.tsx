import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { ReportSummary } from '../../api/reports'
import { ReportsPage } from './ReportsPage'

function report(id: string, name: string, number: number | null, type: ReportSummary['type'], stayEnd: string | null,
                state: ReportSummary['state'] = 'DRAFT'): ReportSummary {
  return {
    id, type, state, stayStart: '2026-07-01', stayEnd, lastUpdate: '2026-10-20T10:00:00Z', writtenSections: 3,
    totalSections: 28,
    resident: { id: `p-${id}`, number, name, surname: 'Prueba', dni: `0000000${id}X`, birthDate: '1990-01-01', sip: null, active: stayEnd === null },
  }
}

const REPORTS = [
  report('1', 'Lucía', 5, 'MID_STAY', null),
  report('2', 'Lucía', 5, 'FINAL', null),
  report('3', 'Óscar', 15, 'MID_STAY', null),
  report('4', 'Pedro', null, 'MID_STAY', '2026-09-30'),
]

function rows() {
  return within(screen.getByRole('table')).getAllByRole('row').slice(1)
}

describe('ReportsPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('muestra por defecto los de las estancias en curso y filtra por residente y tipo', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify(REPORTS), { status: 200, headers: { 'Content-Type': 'application/json' } })))
    render(<MemoryRouter initialEntries={['/informes']}><ReportsPage /></MemoryRouter>)

    // El de Pedro es de una estancia terminada: no sale hasta pedir las terminadas
    expect(await screen.findAllByRole('link', { name: /Lucía Prueba/ })).toHaveLength(2)
    expect(rows()).toHaveLength(3)

    await userEvent.type(screen.getByRole('searchbox'), 'oscar')
    expect(rows()).toHaveLength(1)
    expect(screen.getByRole('link', { name: /Óscar Prueba/ })).toBeInTheDocument()

    await userEvent.clear(screen.getByRole('searchbox'))
    await userEvent.selectOptions(screen.getByLabelText('Tipo'), 'FINAL')
    expect(rows()).toHaveLength(1)

    await userEvent.selectOptions(screen.getByLabelText('Tipo'), 'all')
    await userEvent.selectOptions(screen.getByLabelText('Estancia'), 'finished')
    expect(rows()).toHaveLength(1)
    expect(screen.getByRole('link', { name: /Pedro Prueba/ })).toBeInTheDocument()
  })
})
