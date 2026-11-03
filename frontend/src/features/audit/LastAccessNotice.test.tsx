import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { MyLastAccess } from '../../api/audit'
import { isLastAccessPending, setLastAccessPending } from '../../auth/session'
import { LastAccessNotice } from './LastAccessNotice'

function answer(body: MyLastAccess) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } }))
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

const FIREFOX = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0'

describe('LastAccessNotice', () => {
  beforeEach(() => setLastAccessPending(true))
  afterEach(() => {
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  // ENS op.acc: nada más entrar, cuándo y desde dónde fue el acceso anterior
  it('enseña el acceso anterior y desde dónde', async () => {
    const fetchMock = answer({ previous: { occurredAt: '2026-10-20T07:02:00Z', ipAddress: '192.168.1.37', userAgent: FIREFOX }, failedAttemptsSince: 0 })
    render(<LastAccessNotice />)

    expect(await screen.findByText(/^Tu último acceso fue el/)).toBeInTheDocument()
    expect(screen.getByText('Desde 192.168.1.37 · Firefox · Windows.')).toBeInTheDocument()
    expect(String(fetchMock.mock.calls[0][0])).toBe('/api/audit/access/mine')
  })

  it('avisa de los intentos fallidos con su email desde entonces', async () => {
    answer({ previous: { occurredAt: '2026-10-20T07:02:00Z', ipAddress: null, userAgent: null }, failedAttemptsSince: 3 })
    render(<LastAccessNotice />)

    expect(await screen.findByText(/3 intentos fallidos de entrar con tu email/)).toBeInTheDocument()
  })

  // Sale una vez por entrada: al cerrarlo ya no vuelve aunque se recargue
  it('al cerrarlo no vuelve a salir', async () => {
    answer({ previous: { occurredAt: '2026-10-20T07:02:00Z', ipAddress: null, userAgent: null }, failedAttemptsSince: 0 })
    render(<LastAccessNotice />)

    await userEvent.click(await screen.findByRole('button', { name: 'Cerrar el aviso del último acceso' }))

    expect(screen.queryByText(/^Tu último acceso fue el/)).not.toBeInTheDocument()
    expect(isLastAccessPending()).toBe(false)
  })

  it('sin entrar de nuevo no pregunta nada', () => {
    setLastAccessPending(false)
    const fetchMock = answer({ previous: null, failedAttemptsSince: 0 })
    render(<LastAccessNotice />)

    expect(fetchMock).not.toHaveBeenCalled()
  })
})
