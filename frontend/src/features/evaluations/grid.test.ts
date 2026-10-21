import { describe, expect, it } from 'vitest'
import { average, columnsOf } from './grid'

describe('tabla mensual de valoraciones', () => {
  // Octubre de 2026 empieza en jueves: la primera semana va del 1 al 4 (domingo)
  it('pone la media de cada semana después del domingo y la del mes al final', () => {
    const dates = Array.from({ length: 31 }, (_, i) => `2026-10-${String(i + 1).padStart(2, '0')}`)

    const columns = columnsOf(dates)

    expect(columns.filter((c) => c.kind === 'day')).toHaveLength(31)
    const weeks = columns.filter((c) => c.kind === 'week')
    expect(weeks).toHaveLength(5)
    expect(weeks[0]).toEqual({ kind: 'week', week: 1, from: 0, to: 3 })
    // La última semana se cierra con el último día del mes aunque no sea domingo
    expect(weeks[4]).toEqual({ kind: 'week', week: 5, from: 25, to: 30 })
    expect(columns[4]).toEqual(weeks[0])
    expect(columns.at(-1)).toEqual({ kind: 'month' })
  })

  it('la media solo cuenta las notas que hay, con un decimal', () => {
    const cell = (mark: number | null) => ({ mark, attended: true, activityId: null })

    expect(average([cell(7), cell(8), cell(null), cell(6.5)])).toBe(7.2)
    expect(average([cell(null)])).toBeNull()
    expect(average([])).toBeNull()
  })
})
