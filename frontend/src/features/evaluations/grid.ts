import type { MonthlyCell } from '../../api/diary'
import type { IsoDate } from '../../api/types'

// La tabla mensual de la memoria final: después de cada domingo, la media de esa semana; al final, la del mes.
// Una columna es un día o una media
export type Column =
  | { kind: 'day'; index: number; date: IsoDate; weekday: number }
  | { kind: 'week'; week: number; from: number; to: number }
  | { kind: 'month' }

// new Date('2026-10-20') sería medianoche UTC y en España podría caer en el día anterior
function weekdayOf(iso: IsoDate): number {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).getDay()
}

export function columnsOf(dates: IsoDate[]): Column[] {
  const columns: Column[] = []
  let week = 1
  let from = 0
  dates.forEach((date, index) => {
    const weekday = weekdayOf(date)
    columns.push({ kind: 'day', index, date, weekday })
    // Domingo (0) o último día del mes: se cierra la semana
    if (weekday === 0 || index === dates.length - 1) {
      columns.push({ kind: 'week', week, from, to: index })
      week++
      from = index + 1
    }
  })
  columns.push({ kind: 'month' })
  return columns
}

// La media con un decimal de las notas que haya; null si no hay ninguna
export function average(cells: MonthlyCell[]): number | null {
  const marks = cells.map((cell) => cell.mark).filter((mark): mark is number => mark !== null)
  if (marks.length === 0) return null
  return Math.round((marks.reduce((sum, mark) => sum + mark, 0) / marks.length) * 10) / 10
}
