import type { IsoDate } from '../../api/types'
import type { IsoMonth, PlannedShift, ShiftPart } from '../../api/rota'
import { SHIFT_PARTS } from '../../api/rota'

// Cuentas de calendario del cuadrante. Todo en fechas locales: new Date('2026-10-20') sería medianoche
// UTC y en España podría caer en el día anterior

const pad = (n: number) => String(n).padStart(2, '0')

function iso(date: Date): IsoDate {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function parse(day: IsoDate): Date {
  const [y, m, d] = day.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function daysOf(month: IsoMonth): IsoDate[] {
  const [y, m] = month.split('-').map(Number)
  const count = new Date(y, m, 0).getDate()
  return Array.from({ length: count }, (_, i) => `${month}-${pad(i + 1)}`)
}

// El mes de al lado: -1 el anterior, 1 el siguiente
export function addMonths(month: IsoMonth, delta: number): IsoMonth {
  const [y, m] = month.split('-').map(Number)
  const date = new Date(y, m - 1 + delta, 1)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`
}

// 0 = lunes ... 6 = domingo, como se cuenta la semana en España
export function weekdayOf(day: IsoDate): number {
  return (parse(day).getDay() + 6) % 7
}

export function mondayOf(day: IsoDate): IsoDate {
  const date = parse(day)
  date.setDate(date.getDate() - weekdayOf(day))
  return iso(date)
}

// Las semanas del mes de lunes a domingo; null en los huecos de antes del 1 y después del último día
export function weeksOf(month: IsoMonth): (IsoDate | null)[][] {
  const days: (IsoDate | null)[] = [...Array(weekdayOf(`${month}-01`)).fill(null), ...daysOf(month)]
  while (days.length % 7 !== 0) days.push(null)
  const weeks: (IsoDate | null)[][] = []
  for (let i = 0; i < days.length; i += 7) weeks.push(days.slice(i, i + 7))
  return weeks
}

// Los turnos de un día ordenados mañana, tarde, noche
export function byPart(shifts: PlannedShift[]): PlannedShift[] {
  return [...shifts].sort((a, b) => SHIFT_PARTS.indexOf(a.part) - SHIFT_PARTS.indexOf(b.part))
}

// Índice "empleado|día|parte" para encontrar un turno en la tabla sin recorrer la lista cada vez
export function shiftKey(employeeId: string, date: IsoDate, part: ShiftPart): string {
  return `${employeeId}|${date}|${part}`
}

export function indexShifts(shifts: PlannedShift[]): Map<string, PlannedShift> {
  return new Map(shifts.map((s) => [shiftKey(s.employeeId, s.date, s.part), s]))
}

// Cuántas personas hay en cada parte del día: la fila de cobertura bajo el cuadrante
export function coverage(shifts: PlannedShift[], date: IsoDate, part: ShiftPart): number {
  return shifts.filter((s) => s.date === date && s.part === part).length
}

// "octubre de 2026"; como título, con la primera letra en mayúscula: "Octubre de 2026"
export function monthName(month: IsoMonth, language: string, title = false): string {
  const [y, m] = month.split('-').map(Number)
  const name = new Intl.DateTimeFormat(language === 'en' ? 'en-GB' : 'es-ES', { month: 'long', year: 'numeric' })
    .format(new Date(y, m - 1, 1))
  return title ? name.charAt(0).toUpperCase() + name.slice(1) : name
}
