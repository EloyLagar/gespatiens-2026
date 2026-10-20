import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import type { Activity } from '../../api/diary'
import { ActivitiesSection } from './ActivitiesSection'

function respond(body: unknown) {
  return vi.fn().mockImplementation(async () =>
    new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } }),
  )
}

const resident = (id: string, number: number, name: string) => ({ id, number, name, surname: 'Ruiz' })

const WALK: Activity = {
  id: 'act-1',
  startTime: '10:30:00',
  type: 'WALK',
  title: null,
  attendance: [
    { resident: resident('p1', 1, 'Ana'), status: 'UNEXCUSED_ABSENCE', reduced: false, partialReduction: null },
    { resident: resident('p2', 2, 'Luis'), status: null, reduced: false, partialReduction: 'sin cargar peso' },
    { resident: resident('p3', 3, 'Marta'), status: null, reduced: true, partialReduction: null },
  ],
}

describe('ActivitiesSection', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('resume la lista anotada y cuenta a los rebajados', () => {
    render(<ActivitiesSection date="2000-01-01" activities={[WALK]} onChanged={() => {}} />)
    expect(screen.getByRole('heading', { name: 'Paseo' })).toBeInTheDocument()
    expect(screen.getByText('1 falta sin justificar · 1 rebajado · 1 sin anotar')).toBeInTheDocument()
  })

  it('pasa lista sin anotar a los rebajados', async () => {
    const fetchMock = respond({ ...WALK, attendance: WALK.attendance })
    vi.stubGlobal('fetch', fetchMock)
    const onChanged = vi.fn()
    render(<ActivitiesSection date="2000-01-01" activities={[WALK]} onChanged={onChanged} />)

    await userEvent.click(screen.getByRole('button', { name: 'Revisar lista' }))
    expect(screen.getByText('Rebajado')).toBeInTheDocument()
    expect(screen.getByText('Rebaja parcial: sin cargar peso')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Marcar al resto como asistentes' }))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar lista' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/diary/activities/act-1/attendance')
    expect(JSON.parse(init.body)).toEqual({ attendance: { p1: 'UNEXCUSED_ABSENCE', p2: 'ATTENDED' } })
  })

  it('no deja pasar lista de un día que no ha llegado', () => {
    render(<ActivitiesSection date="2999-01-01" activities={[WALK]} onChanged={() => {}} />)
    expect(screen.queryByRole('button', { name: /lista/ })).not.toBeInTheDocument()
  })

  it('pide el nombre del grupo terapéutico antes de guardar', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    render(<ActivitiesSection date="2000-01-01" activities={[]} onChanged={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Programar actividad' }))
    await userEvent.type(screen.getByLabelText('Hora'), '17:00')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(screen.getByText('Indica el nombre del grupo terapéutico.')).toHaveAttribute('role', 'alert')
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
