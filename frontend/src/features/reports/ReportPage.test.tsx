import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { EditLock } from '../../api/editLocks'
import type { Report, ReportSection } from '../../api/reports'
import { ReportPage } from './ReportPage'

const marcos = { id: 'u1', name: 'Marcos', speciality: 'PSYCHOLOGIST' as const }
const elena = { id: 'u2', name: 'Elena', speciality: 'PSYCHOLOGIST' as const }
const DIAGNOSIS = 'Diagnóstico (Eje I y II en DSM-IV)'

function section(code: string, area: ReportSection['area'], text: string | null = null, version = 0): ReportSection {
  return {
    section: code,
    area,
    adaptation: false,
    text,
    version,
    author: text ? marcos : null,
    updatedAt: text ? '2026-10-20T09:00:00Z' : null,
  }
}

function report(overrides: Partial<Report> = {}): Report {
  return {
    id: 'r1',
    type: 'MID_STAY',
    state: 'DRAFT',
    resident: { id: 'p1', number: 5, name: 'Lucía', surname: 'Martínez', dni: '12345678Z', birthDate: '1990-05-03', sip: null, active: true },
    stayStart: '2026-04-01',
    header: { requestNumber: null, referenceCenter: 'Comunidad Terapéutica Los Vientos', nip: null, entryDate: '2026-04-01', exitDate: null, exitReason: null, version: 0 },
    areas: [
      { area: 'HEALTH', writers: ['MEDICAL_TEAM'], tutors: false, writable: false },
      { area: 'PSYCHOLOGICAL', writers: ['PSYCHOLOGIST'], tutors: false, writable: true },
    ],
    sections: [section('HEALTH_AT_ENTRY', 'HEALTH'), section('PSYCHO_DIAGNOSIS', 'PSYCHOLOGICAL', 'Dual', 1)],
    authors: [marcos],
    createdBy: null,
    createdAt: '2026-04-01T09:00:00Z',
    closedBy: null,
    closedAt: null,
    writtenSections: 1,
    totalSections: 2,
    manageable: false,
    ...overrides,
  }
}

function lock(resource: string, mine: boolean, holderName = 'Elena'): EditLock {
  return { resource, holderId: mine ? 'u1' : 'u2', holderName, since: '2026-10-21T10:00:00Z', expiresAt: '2026-10-21T10:01:30Z', mine }
}

type Reply = { status: number; body?: unknown }

// Responde según la URL y el método. Cada PUT de un apartado se lleva la siguiente respuesta de la lista
function api(page: Report, { saves = [] as Reply[], acquire = null as Reply | null, locks = [] as EditLock[] } = {}) {
  return vi.fn().mockImplementation(async (url: string, init?: RequestInit) => {
    const method = init?.method ?? 'GET'
    let reply: Reply
    if (url.startsWith('/api/edit-locks?')) reply = { status: 200, body: locks }
    else if (url.startsWith('/api/edit-locks/') && method === 'PUT') {
      reply = acquire ?? { status: 200, body: lock(decodeURIComponent(url.split('/').pop()!), true, 'Marcos') }
    } else if (url.startsWith('/api/edit-locks/')) reply = { status: 204 }
    else if (method === 'PUT') reply = saves.shift()!
    else reply = { status: 200, body: page }
    return new Response(reply.body === undefined ? null : JSON.stringify(reply.body), {
      status: reply.status,
      headers: { 'Content-Type': 'application/json' },
    })
  })
}

function renderPage() {
  render(
    <MemoryRouter initialEntries={['/informes/r1']}>
      <Routes>
        <Route path="/informes/:id" element={<ReportPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

const sectionPuts = (fetchMock: ReturnType<typeof api>) =>
  fetchMock.mock.calls.filter(([url, init]) => String(url).startsWith('/api/reports/') && init?.method === 'PUT')

describe('ReportPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('cada especialidad redacta su área y lee las demás', async () => {
    vi.stubGlobal('fetch', api(report()))
    renderPage()

    expect(await screen.findByRole('button', { name: `Editar «${DIAGNOSIS}»` })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Situación sanitaria al ingreso' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar «Situación sanitaria al ingreso»' })).not.toBeInTheDocument()
    expect(screen.getByText('La redacta: Equipo médico, Administración')).toBeInTheDocument()
    expect(screen.getByText('Sin redactar')).toBeInTheDocument()
  })

  // RF8: al pulsar Editar se pide el bloqueo; al guardar se manda la versión de partida y se suelta
  it('edita un apartado bajo bloqueo y lo guarda con su versión', async () => {
    const saved = report({ sections: [section('HEALTH_AT_ENTRY', 'HEALTH'), section('PSYCHO_DIAGNOSIS', 'PSYCHOLOGICAL', 'Dual. Revisado', 2)] })
    const fetchMock = api(report(), { saves: [{ status: 200, body: saved }] })
    vi.stubGlobal('fetch', fetchMock)
    renderPage()
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: `Editar «${DIAGNOSIS}»` }))
    await user.type(await screen.findByLabelText(DIAGNOSIS), '. Revisado')
    await user.click(screen.getByRole('button', { name: 'Guardar apartado' }))

    expect(await screen.findByText('Guardado')).toBeInTheDocument()
    expect(screen.getByText('Dual. Revisado')).toBeInTheDocument()
    const [url, init] = sectionPuts(fetchMock)[0]
    expect(url).toBe('/api/reports/r1/sections/PSYCHO_DIAGNOSIS')
    expect(JSON.parse(init.body)).toEqual({ text: 'Dual. Revisado', version: 1 })
    const methods = fetchMock.mock.calls.filter(([u]) => String(u).startsWith('/api/edit-locks/')).map(([, i]) => i?.method)
    expect(methods).toEqual(['PUT', 'DELETE'])
  })

  it('si otra persona lo tiene abierto no deja editarlo y dice quién es', async () => {
    vi.stubGlobal('fetch', api(report(), { acquire: { status: 409, body: { detail: 'Elena lo está editando', beingEditedBy: 'Elena' } } }))
    renderPage()
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: `Editar «${DIAGNOSIS}»` }))

    expect(await screen.findByText('Elena lo está editando ahora mismo. Podrás editarlo cuando termine.')).toBeInTheDocument()
    expect(screen.queryByLabelText(DIAGNOSIS)).not.toBeInTheDocument()
  })

  it('avisa de lo que están editando otras personas antes de intentarlo', async () => {
    vi.stubGlobal('fetch', api(report(), { locks: [lock('report:r1:section:PSYCHO_DIAGNOSIS', false)] }))
    renderPage()

    expect(await screen.findByText('Elena lo está editando')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: `Editar «${DIAGNOSIS}»` })).not.toBeInTheDocument()
  })

  // Si aun así el apartado cambió (el bloqueo caducó), se enseñan las dos versiones para elegir
  it('ante un choque enseña la otra versión y permite guardar la propia encima', async () => {
    const conflict = { status: 409, detail: 'Otra persona ha modificado...', section: 'PSYCHO_DIAGNOSIS', currentText: 'Lo de Elena', currentVersion: 2 }
    const saved = report({
      sections: [section('HEALTH_AT_ENTRY', 'HEALTH'), { ...section('PSYCHO_DIAGNOSIS', 'PSYCHOLOGICAL', 'Dual. Mío', 3), author: elena }],
    })
    const fetchMock = api(report(), { saves: [{ status: 409, body: conflict }, { status: 200, body: saved }] })
    vi.stubGlobal('fetch', fetchMock)
    renderPage()
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: `Editar «${DIAGNOSIS}»` }))
    await user.type(await screen.findByLabelText(DIAGNOSIS), '. Mío')
    await user.click(screen.getByRole('button', { name: 'Guardar apartado' }))

    expect(await screen.findByText('Lo de Elena')).toBeInTheDocument()
    expect(screen.getByLabelText(DIAGNOSIS)).toHaveValue('Dual. Mío')
    await user.click(screen.getByRole('button', { name: 'Guardar la mía encima' }))

    await waitFor(() => expect(screen.queryByText('Lo de Elena')).not.toBeInTheDocument())
    expect(JSON.parse(sectionPuts(fetchMock)[1][1].body)).toEqual({ text: 'Dual. Mío', version: 2 })
  })

  it('un informe cerrado solo se lee', async () => {
    vi.stubGlobal('fetch', api(report({ state: 'CLOSED', closedAt: '2026-10-20T09:00:00Z', closedBy: { id: 'a', name: 'Rosa', speciality: 'ADMINISTRATOR' } })))
    renderPage()

    expect(await screen.findByText(/Informe cerrado/)).toHaveTextContent('Rosa')
    expect(screen.queryByRole('button', { name: /^Editar/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar cabecera' })).not.toBeInTheDocument()
  })
})
