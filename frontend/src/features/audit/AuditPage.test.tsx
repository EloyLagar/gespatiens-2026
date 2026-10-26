import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { AuditPage as AuditPageData } from '../../api/audit'
import { AuditPage } from './AuditPage'

const PAGE: AuditPageData = {
  page: 0, totalPages: 1, totalEntries: 3,
  entries: [
    { id: 'a1', occurredAt: '2026-10-21T09:15:00Z', action: 'EDIT_REFERRAL', actor: { id: 'u1', name: 'Ana' },
      resident: { id: 'p5', number: 5, name: 'Lucía', surname: 'Martínez' }, resourceId: null },
    { id: 'a2', occurredAt: '2026-10-20T09:15:00Z', action: 'CLOSE_REPORT', actor: { id: 'u9', name: null },
      resident: { id: 'p5', number: 5, name: 'Lucía', surname: 'Martínez' }, resourceId: 'r1' },
    { id: 'a3', occurredAt: '2026-10-19T09:15:00Z', action: 'EDIT_RECORD', actor: { id: 'u1', name: 'Ana' },
      resident: { id: 'p0', number: null, name: null, surname: null }, resourceId: null },
  ],
}

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}

describe('AuditPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  // RNF7: quién ha cambiado los datos de un residente; la ficha enlaza con ?residente=
  it('lista los cambios del residente que viene en la URL', async () => {
    const fetchMock = vi.fn().mockImplementation(async (url: string) =>
      url.startsWith('/api/audit') ? json(PAGE) : json([]))
    vi.stubGlobal('fetch', fetchMock)
    render(<MemoryRouter initialEntries={['/registro?residente=p5']}><AuditPage /></MemoryRouter>)

    expect(await screen.findByText('Cambió la derivación')).toBeInTheDocument()
    expect(screen.getByText('3 cambios')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Cerró un informe' })[0]).toHaveAttribute('href', '/informes/r1')
    expect(screen.getByText('Ya no está en la plantilla')).toBeInTheDocument()
    expect(screen.getAllByText('Ficha borrada')[0]).toBeInTheDocument()
    const auditCall = fetchMock.mock.calls.map((c) => String(c[0])).find((u) => u.startsWith('/api/audit'))
    expect(auditCall).toContain('patientId=p5')
  })
})
