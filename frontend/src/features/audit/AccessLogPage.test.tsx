import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { AccessPage } from '../../api/audit'
import { AccessLogPage, describeBrowser } from './AccessLogPage'

const FIREFOX = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0'

const PAGE: AccessPage = {
  page: 0, totalPages: 1, totalEntries: 4,
  entries: [
    { id: 'a1', occurredAt: '2026-10-21T09:15:00Z', email: 'ana@vientos.es', employee: { id: 'u1', name: 'Ana' }, actor: null,
      outcome: 'LOCKED', ipAddress: '192.168.1.37', userAgent: FIREFOX },
    { id: 'a2', occurredAt: '2026-10-21T09:14:00Z', email: 'admin@vientos.es', employee: null, actor: null,
      outcome: 'FAILURE', ipAddress: '10.0.0.8', userAgent: null },
    { id: 'a3', occurredAt: '2026-10-20T08:00:00Z', email: 'ex@vientos.es', employee: { id: 'u9', name: null }, actor: null,
      outcome: 'SUCCESS', ipAddress: null, userAgent: null },
    { id: 'a4', occurredAt: '2026-10-19T08:00:00Z', email: 'luis@vientos.es', employee: { id: 'u2', name: 'Luis' },
      actor: { id: 'u0', name: 'Marta' }, outcome: 'PASSWORD_RESET', ipAddress: '10.0.0.2', userAgent: null },
  ],
}

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}

describe('AccessLogPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  // ENS: quién ha entrado, desde dónde y quién lo ha intentado sin conseguirlo
  it('lista los intentos fallidos con el resultado y desde dónde', async () => {
    const fetchMock = vi.fn().mockImplementation(async (url: string) =>
      url.startsWith('/api/audit/access') ? json(PAGE) : json([]))
    vi.stubGlobal('fetch', fetchMock)
    render(<MemoryRouter initialEntries={['/registro/accesos?resultado=failed']}><AccessLogPage /></MemoryRouter>)

    expect(await screen.findAllByText('Bloqueado tras 5 fallos')).not.toHaveLength(0)
    expect(screen.getByText('4 accesos')).toBeInTheDocument()
    expect(screen.getAllByText('192.168.1.37 · Firefox · Windows')[0]).toBeInTheDocument()
    expect(screen.getAllByText('No es el email de nadie de la plantilla')[0]).toBeInTheDocument()
    expect(screen.getByText('Ya no está en la plantilla')).toBeInTheDocument()
    expect(screen.getAllByText('Sin datos')[0]).toBeInTheDocument()
    // La contraseña provisional dice quién de la administración la puso
    expect(screen.getAllByText('Le pusieron una provisional')[0]).toBeInTheDocument()
    expect(screen.getAllByText('Por Marta')[0]).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Accesos' })).toHaveAttribute('aria-current', 'page')

    const call = fetchMock.mock.calls.map((c) => String(c[0])).find((u) => u.startsWith('/api/audit/access'))
    expect(call).toContain('outcome=FAILURE')
    expect(call).toContain('outcome=LOCKED')
    expect(call).toContain('outcome=BLOCKED')
  })

  it('resume el navegador y el sistema', () => {
    expect(describeBrowser(FIREFOX)).toBe('Firefox · Windows')
    expect(describeBrowser('Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1'))
      .toBe('Safari · iOS')
    expect(describeBrowser('Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/130.0 Safari/537.36 Edg/130.0'))
      .toBe('Edge · Windows')
    expect(describeBrowser(null)).toBeNull()
  })
})
