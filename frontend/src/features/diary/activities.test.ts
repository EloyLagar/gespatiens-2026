import { describe, expect, it } from 'vitest'
import type { Activity, AttendanceRow } from '../../api/diary'
import { activityName, attendanceOf, byStartTime, summarize } from './activities'

const t = (key: string) => key

function row(id: string, status: AttendanceRow['status'], reduced = false): AttendanceRow {
  return { resident: { id, number: 1, name: 'Ana', surname: 'Ruiz' }, status, reduced, partialReduction: null }
}

describe('activityName', () => {
  it('junta el tipo y el nombre del grupo', () => {
    expect(activityName({ type: 'THERAPEUTIC_GROUP', title: 'Autoestima' }, t)).toBe(
      'diary.activities.type.THERAPEUTIC_GROUP · Autoestima',
    )
  })

  it('usa solo el tipo si no hay nombre', () => {
    expect(activityName({ type: 'WALK', title: null }, t)).toBe('diary.activities.type.WALK')
  })

  it('en "Otros" el nombre basta', () => {
    expect(activityName({ type: 'OTHER', title: 'Taller de cerámica' }, t)).toBe('Taller de cerámica')
  })
})

describe('summarize', () => {
  it('cuenta cada estado, los rebajados y quienes faltan por anotar', () => {
    const rows = [row('a', 'ATTENDED'), row('b', 'ATTENDED'), row('c', 'UNEXCUSED_ABSENCE'), row('d', null, true), row('e', null)]
    expect(summarize(rows)).toEqual({ ATTENDED: 2, EXCUSED_ABSENCE: 0, UNEXCUSED_ABSENCE: 1, reduced: 1, pending: 1 })
  })
})

describe('attendanceOf', () => {
  it('manda solo lo anotado y nunca a los rebajados', () => {
    expect(attendanceOf([row('a', 'ATTENDED'), row('b', null), row('c', 'EXCUSED_ABSENCE', true)])).toEqual({ a: 'ATTENDED' })
  })
})

describe('byStartTime', () => {
  it('ordena por hora', () => {
    const at = (id: string, startTime: string) => ({ id, startTime }) as Activity
    expect(byStartTime([at('b', '17:00'), at('a', '09:30:00')]).map((a) => a.id)).toEqual(['a', 'b'])
  })
})
