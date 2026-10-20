import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { FollowUp, Intervention } from '../../api/interventions'
import { todayIso } from '../../shared/dates'
import { FollowUpPage } from './FollowUpPage'

const auth = vi.hoisted(() => ({ isAdmin: false }))
vi.mock('../../auth/AuthContext', () => ({ useAuth: () => auth }))

const pablo = { id: 'u1', name: 'Pablo', speciality: 'SOCIAL_EDUCATOR' as const }
const ana = { id: 'u2', name: 'Ana', speciality: 'PSYCHOLOGIST' as const }

const OLD: Intervention = {
  id: 'i1',
  date: '2026-10-01',
  text: 'Primera tutoría',
  createdAt: '2026-10-01T09:00:00Z',
  editedAt: null,
  author: ana,
  mine: false,
  manageable: false,
}

function followUp(overrides: Partial<FollowUp> = {}): FollowUp {
  return {
    resident: { id: 'p1', number: 5, name: 'Lucía', surname: 'Martínez', active: true },
    tutors: [ana, pablo],
    interventions: [OLD],
    tutor: true,
    canRecord: true,
    ...overrides,
  }
}

// Responde según la URL: el seguimiento al cargar y lo que devuelva la API al guardar
function api(page: FollowUp, saved?: Intervention) {
  return vi.fn().mockImplementation(async (url: string, init?: RequestInit) => {
    const body = init?.method === 'POST' ? saved : page
    return new Response(JSON.stringify(body), { status: init?.method === 'POST' ? 201 : 200,
      headers: { 'Content-Type': 'application/json' } })
  })
}

function renderPage() {
  render(
    <MemoryRouter initialEntries={['/intervenciones/p1']}>
      <Routes>
        <Route path="/intervenciones/:patientId" element={<FollowUpPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('FollowUpPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    auth.isAdmin = false
  })

  it('un tutor registra una intervención y aparece la primera', async () => {
    const saved: Intervention = { ...OLD, id: 'i2', date: todayIso(), text: 'Entrevista con la familia',
      createdAt: '2026-10-21T10:00:00Z', author: pablo, mine: true, manageable: true }
    const fetchMock = api(followUp(), saved)
    vi.stubGlobal('fetch', fetchMock)
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Lucía Martínez' })).toBeInTheDocument()
    expect(screen.getByText('Ana')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Intervención realizada'), 'Entrevista con la familia')
    await userEvent.click(screen.getByRole('button', { name: 'Registrar intervención' }))

    await waitFor(() => expect(screen.getAllByRole('article')[0]).toHaveTextContent('Entrevista con la familia'))
    const [url, init] = fetchMock.mock.calls[1]
    expect(url).toBe('/api/interventions/residents/p1')
    expect(JSON.parse(init.body)).toEqual({ date: todayIso(), text: 'Entrevista con la familia' })
    expect(screen.getByLabelText('Intervención realizada')).toHaveValue('')
  })

  it('quien no es tutor solo consulta el historial', async () => {
    vi.stubGlobal('fetch', api(followUp({ tutor: false, canRecord: false })))
    renderPage()

    expect(await screen.findByText('Solo sus tutores pueden registrar intervenciones.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Intervención realizada')).not.toBeInTheDocument()
    expect(screen.getByText('Primera tutoría')).toBeInTheDocument()
    // El diálogo de borrado de Carbon siempre está montado: se mira la acción de la propia intervención
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cambiar tutores' })).not.toBeInTheDocument()
  })

  it('la administración puede cambiar los tutores', async () => {
    auth.isAdmin = true
    vi.stubGlobal('fetch', api(followUp({ tutors: [], tutor: false, canRecord: false })))
    renderPage()

    expect(await screen.findByRole('button', { name: 'Cambiar tutores' })).toBeInTheDocument()
    expect(screen.getByText(/Asígnaselos para que puedan registrar/)).toBeInTheDocument()
  })
})
