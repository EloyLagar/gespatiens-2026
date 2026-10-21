import { describe, expect, it } from 'vitest'
import { addMonths, byPart, daysOf, mondayOf, weekdayOf, weeksOf } from './calendar'

describe('calendario del cuadrante', () => {
  it('cuenta los días del mes, también en febrero', () => {
    expect(daysOf('2026-10')).toHaveLength(31)
    expect(daysOf('2028-02').at(-1)).toBe('2028-02-29')
  })

  it('pasa de mes cambiando de año', () => {
    expect(addMonths('2026-12', 1)).toBe('2027-01')
    expect(addMonths('2026-01', -1)).toBe('2025-12')
  })

  it('la semana empieza en lunes', () => {
    expect(weekdayOf('2026-10-19')).toBe(0)
    expect(weekdayOf('2026-10-25')).toBe(6)
    expect(mondayOf('2026-10-25')).toBe('2026-10-19')
    expect(mondayOf('2026-11-01')).toBe('2026-10-26')
  })

  it('octubre de 2026 empieza en jueves y ocupa cinco semanas', () => {
    const weeks = weeksOf('2026-10')
    expect(weeks).toHaveLength(5)
    expect(weeks[0]).toEqual([null, null, null, '2026-10-01', '2026-10-02', '2026-10-03', '2026-10-04'])
    expect(weeks[4].filter(Boolean)).toEqual(['2026-10-26', '2026-10-27', '2026-10-28', '2026-10-29', '2026-10-30', '2026-10-31'])
  })

  it('ordena los turnos del día: mañana, tarde y noche', () => {
    const shift = (part: 'MORNING' | 'AFTERNOON' | 'NIGHT') => ({ id: part, date: '2026-10-21', part, employeeId: 'e' })
    expect(byPart([shift('NIGHT'), shift('MORNING'), shift('AFTERNOON')]).map((s) => s.part))
      .toEqual(['MORNING', 'AFTERNOON', 'NIGHT'])
  })
})
