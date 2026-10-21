import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { Referral } from '../../api/patients'
import { ReferralPanel } from './ReferralPanel'

const EMPTY: Referral = {
  referringInstitution: null, contactName: null, contactPhone: null, courtOrdered: false,
  court: null, measureEndDate: null, notes: null, updatedAt: '2026-10-21T10:00:00Z',
}

function api(page: Referral) {
  return vi.fn().mockImplementation(async (_url: string, init?: RequestInit) =>
    new Response(init?.method === 'PUT' ? init.body as string : JSON.stringify(page), {
      status: 200, headers: { 'Content-Type': 'application/json' },
    }))
}

describe('ReferralPanel', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('el personal la consulta sin poder cambiarla', async () => {
    vi.stubGlobal('fetch', api({ ...EMPTY, referringInstitution: 'UCA Torrent', courtOrdered: true, court: 'Juzgado nº 3' }))
    render(<ReferralPanel patientId="p1" canManage={false} />)

    expect(await screen.findByText('UCA Torrent')).toBeInTheDocument()
    expect(screen.getByText('Juzgado nº 3')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument()
  })

  // El juzgado y el fin de la medida solo aparecen si el ingreso es por medida judicial
  it('la administración la completa, con la medida judicial si la hay', async () => {
    const fetchMock = api(EMPTY)
    vi.stubGlobal('fetch', fetchMock)
    render(<ReferralPanel patientId="p1" canManage />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Editar' }))
    await user.type(screen.getByLabelText('Centro o institución que lo deriva'), 'UCA Torrent')
    expect(screen.queryByLabelText('Juzgado y nº de procedimiento')).not.toBeInTheDocument()
    await user.click(screen.getByLabelText('Ingresa por una medida judicial'))
    await user.type(screen.getByLabelText('Juzgado y nº de procedimiento'), 'Juzgado nº 3')
    await user.click(screen.getByRole('button', { name: 'Guardar' }))

    await waitFor(() => expect(fetchMock.mock.calls.some(([, i]) => i?.method === 'PUT')).toBe(true))
    const [url, init] = fetchMock.mock.calls.find(([, i]) => i?.method === 'PUT')!
    expect(url).toBe('/api/patients/p1/referral')
    expect(JSON.parse(init.body)).toMatchObject({ referringInstitution: 'UCA Torrent', courtOrdered: true, court: 'Juzgado nº 3' })
    expect(await screen.findByText('UCA Torrent')).toBeInTheDocument()
  })
})
