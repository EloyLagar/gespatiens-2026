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
    { resident: resident('p1', 1, 'Ana'), status: 'UNEXCUSED_ABSENCE', reduced: false, partialReduction: null, evaluation: null },
    { resident: resident('p2', 2, 'Luis'), status: null, reduced: false, partialReduction: 'sin cargar peso', evaluation: null },
    { resident: resident('p3', 3, 'Marta'), status: null, reduced: true, partialReduction: null, evaluation: null },
  ],
  averages: { mark: null, satisfaction: null, utility: null },
}

const FORUM: Activity = {
  id: 'act-2',
  startTime: '17:00:00',
  type: 'VIDEO_FORUM',
  title: null,
  attendance: [
    { resident: resident('p1', 1, 'Ana'), status: 'ATTENDED', reduced: false, partialReduction: null,
      evaluation: { mark: 7.5, satisfaction: 9, utility: null } },
    { resident: resident('p2', 2, 'Luis'), status: 'ATTENDED', reduced: false, partialReduction: null, evaluation: null },
    { resident: resident('p3', 3, 'Marta'), status: 'EXCUSED_ABSENCE', reduced: false, partialReduction: null, evaluation: null },
  ],
  averages: { mark: 7.5, satisfaction: 9, utility: null },
}

describe('ActivitiesSection', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('resume la lista anotada y cuenta a los rebajados', () => {
    render(<ActivitiesSection canEvaluate date="2000-01-01" activities={[WALK]} onChanged={() => {}} />)
    expect(screen.getByRole('heading', { name: 'Paseo' })).toBeInTheDocument()
    expect(screen.getByText('1 falta sin justificar · 1 rebajado · 1 sin anotar')).toBeInTheDocument()
  })

  it('pasa lista sin anotar a los rebajados', async () => {
    const fetchMock = respond({ ...WALK, attendance: WALK.attendance })
    vi.stubGlobal('fetch', fetchMock)
    const onChanged = vi.fn()
    render(<ActivitiesSection canEvaluate date="2000-01-01" activities={[WALK]} onChanged={onChanged} />)

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
    render(<ActivitiesSection canEvaluate date="2999-01-01" activities={[WALK]} onChanged={() => {}} />)
    expect(screen.queryByRole('button', { name: /lista/ })).not.toBeInTheDocument()
  })

  it('pide el nombre del grupo terapéutico antes de guardar', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    render(<ActivitiesSection canEvaluate date="2000-01-01" activities={[]} onChanged={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Programar actividad' }))
    await userEvent.type(screen.getByLabelText('Hora'), '17:00')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(screen.getByText('Indica el nombre del grupo terapéutico.')).toHaveAttribute('role', 'alert')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('muestra las medias y valora solo a quienes asistieron, con coma decimal', async () => {
    const fetchMock = respond(FORUM)
    vi.stubGlobal('fetch', fetchMock)
    const onChanged = vi.fn()
    render(<ActivitiesSection canEvaluate date="2000-01-01" activities={[FORUM]} onChanged={onChanged} />)
    expect(screen.getByText('Medias: nota 7,5 · satisfacción 9')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Revisar valoraciones' }))
    expect(screen.queryByText('Marta Ruiz')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Nota de Ana Ruiz')).toHaveValue('7,5')
    await userEvent.type(screen.getByLabelText('Nota de Luis Ruiz'), '6,5')
    await userEvent.type(screen.getByLabelText('Utilidad de Luis Ruiz'), '10')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar valoraciones' }))

    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/diary/activities/act-2/evaluations')
    expect(JSON.parse(init.body)).toEqual({
      evaluations: {
        p1: { mark: 7.5, satisfaction: 9, utility: null },
        p2: { mark: 6.5, satisfaction: null, utility: 10 },
      },
    })
  })

  it('no manda puntuaciones fuera de 0 a 10', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    render(<ActivitiesSection canEvaluate date="2000-01-01" activities={[FORUM]} onChanged={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Revisar valoraciones' }))
    await userEvent.type(screen.getByLabelText('Nota de Luis Ruiz'), '11')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar valoraciones' }))

    expect(screen.getByLabelText('Nota de Luis Ruiz')).toHaveAttribute('aria-invalid', 'true')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('quien no es educador ni administración solo ve las valoraciones', async () => {
    render(<ActivitiesSection canEvaluate={false} date="2000-01-01" activities={[FORUM]} onChanged={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Ver valoraciones' }))
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Guardar valoraciones' })).not.toBeInTheDocument()
  })
})
