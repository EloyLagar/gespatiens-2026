import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { Visitor } from '../../api/patients'
import { VisitorsPanel } from './VisitorsPanel'

const ROSA: Visitor = { id: 'v1', name: 'Rosa', relationship: 'Madre', phoneNumber: '600000001' }

function api(list: Visitor[], saved?: Visitor) {
  return vi.fn().mockImplementation(async (_url: string, init?: RequestInit) =>
    new Response(JSON.stringify(init?.method === 'POST' ? saved : list), {
      status: init?.method === 'POST' ? 201 : 200,
      headers: { 'Content-Type': 'application/json' },
    }))
}

describe('VisitorsPanel', () => {
  afterEach(() => vi.unstubAllGlobals())

  // Todo el personal ve a quién dejar pasar; gestionarlo es de la administración
  it('el personal ve a las personas autorizadas pero no las gestiona', async () => {
    vi.stubGlobal('fetch', api([ROSA]))
    render(<VisitorsPanel patientId="p1" canManage={false} />)

    expect(await screen.findByText('Rosa')).toBeInTheDocument()
    expect(screen.getByText('Madre')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '600000001' })).toHaveAttribute('href', 'tel:600000001')
    expect(screen.queryByRole('button', { name: 'Añadir persona' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Quitar a Rosa' })).not.toBeInTheDocument()
  })

  it('la administración autoriza a una persona con su relación y teléfono', async () => {
    const luis = { id: 'v2', name: 'Luis', relationship: 'Hermano', phoneNumber: '600000002' }
    const fetchMock = api([], luis)
    vi.stubGlobal('fetch', fetchMock)
    render(<VisitorsPanel patientId="p1" canManage />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Añadir persona' }))
    await user.click(screen.getByRole('button', { name: 'Autorizar' }))
    expect(screen.getAllByText('Este campo es obligatorio.')).toHaveLength(3)

    await user.type(screen.getByLabelText('Nombre'), 'Luis')
    await user.type(screen.getByLabelText('Relación familiar'), 'Hermano')
    await user.type(screen.getByLabelText('Teléfono'), '600000002')
    await user.click(screen.getByRole('button', { name: 'Autorizar' }))

    expect(await screen.findByText('Luis')).toBeInTheDocument()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    const [url, init] = fetchMock.mock.calls[1]
    expect(url).toBe('/api/patients/p1/visitors')
    expect(JSON.parse(init.body)).toEqual({ name: 'Luis', relationship: 'Hermano', phoneNumber: '600000002' })
  })
})
