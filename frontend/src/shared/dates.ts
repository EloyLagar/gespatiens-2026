import type { IsoDate } from '../api/types'

// La API trabaja con yyyy-MM-dd; en pantalla se muestran en el formato del idioma (dd/mm/aaaa)

export function todayIso(now: Date = new Date()): IsoDate {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

// new Date('2026-10-20') sería medianoche UTC y en España mostraría el día anterior según la hora
function parse(iso: IsoDate): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function formatDate(iso: IsoDate | null, language: string): string {
  if (!iso) return '—'
  return new Intl.DateTimeFormat(language === 'en' ? 'en-GB' : 'es-ES', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(parse(iso))
}

export function ageOn(birthDate: IsoDate, on: IsoDate = todayIso()): number {
  const birth = parse(birthDate)
  const day = parse(on)
  let age = day.getFullYear() - birth.getFullYear()
  const birthdayPending =
    day.getMonth() < birth.getMonth() || (day.getMonth() === birth.getMonth() && day.getDate() < birth.getDate())
  if (birthdayPending) age--
  return age
}

// Días de estancia contando el de entrada: quien entra y sale el mismo día ha estado 1 día
export function daysBetween(from: IsoDate, to: IsoDate = todayIso()): number {
  const ms = parse(to).getTime() - parse(from).getTime()
  return Math.round(ms / 86_400_000) + 1
}
