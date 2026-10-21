import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { FollowUp, Intervention } from '../../api/interventions'
import { todayIso } from '../../shared/dates'
import { FollowUpPage } from './FollowUpPage'

const auth = vi.hoisted(() => ({ isAdmin: false, user: { speciality: 'PSYCHOLOGIST' } }))
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

const STAFF = [
  { id: 'u1', name: 'Pablo', email: 'p@v.es', phoneNumber: '1', speciality: 'SOCIAL_EDUCATOR' },
  { id: 'u2', name: 'Ana', email: 'a@v.es', phoneNumber: '2', speciality: 'PSYCHOLOGIST' },
  { id: 'u3', name: 'Rosa', email: 'r@v.es', phoneNumber: '3', speciality: 'ADMINISTRATOR' },
]

// Responde según la URL: la plantilla, el seguimiento al cargar y lo que devuelva la API al guardar
function api(page: FollowUp, saved?: unknown) {
  return vi.fn().mockImplementation(async (url: string, init?: RequestInit) => {
    const body = url === '/api/users' ? STAFF : init?.method === 'POST' || init?.method === 'PUT' ? saved : page
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
    expect(screen.getByText(/Asígnale un educador social y un psicólogo/)).toBeInTheDocument()
  })

  // Memoria final: un tutor educador social y un tutor psicólogo, cada uno elegido entre los de su especialidad
  it('se asigna un educador social y un psicólogo', async () => {
    auth.isAdmin = true
    const fetchMock = api(followUp({ tutors: [], tutor: false, canRecord: false }), [pablo, ana])
    vi.stubGlobal('fetch', fetchMock)
    renderPage()
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Cambiar tutores' }))
    const educator = await screen.findByLabelText('Tutor/a educador/a social')
    await waitFor(() => expect(educator).toHaveTextContent('Pablo'))
    expect(educator).not.toHaveTextContent('Ana')
    await user.selectOptions(educator, 'u1')
    await user.selectOptions(screen.getByLabelText('Tutor/a psicólogo/a'), 'u2')
    await user.click(screen.getByRole('button', { name: 'Guardar' }))

    await waitFor(() => expect(fetchMock.mock.calls.some(([, init]) => init?.method === 'PUT')).toBe(true))
    const [url, init] = fetchMock.mock.calls.find(([, i]) => i?.method === 'PUT')!
    expect(url).toBe('/api/interventions/residents/p1/tutors')
    expect(JSON.parse(init.body)).toEqual({ educatorId: 'u1', psychologistId: 'u2' })
  })
})
