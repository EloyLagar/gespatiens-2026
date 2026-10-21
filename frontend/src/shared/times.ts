// Cuándo se publicó un aviso, dicho como lo diría alguien del centro: "hace 5 min", "hoy, 09:30",
// "ayer, 18:10" o la fecha completa. El texto exacto queda en el atributo dateTime del <time>

export type Translate = (key: string, options?: Record<string, unknown>) => string

function sameDay(a: Date, b: Date) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

export function whenPosted(iso: string, t: Translate, language: string, now: Date = new Date()): string {
  const date = new Date(iso)
  const minutes = Math.floor((now.getTime() - date.getTime()) / 60_000)
  const locale = language === 'en' ? 'en-GB' : 'es-ES'
  const time = new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit' }).format(date)

  if (minutes < 1) return t('time.justNow')
  if (minutes < 60) return t('time.minutesAgo', { count: minutes })
  if (sameDay(date, now)) return t('time.today', { time })
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (sameDay(date, yesterday)) return t('time.yesterday', { time })
  const day = new Intl.DateTimeFormat(locale, { day: '2-digit', month: '2-digit', year: 'numeric' }).format(date)
  return t('time.onDay', { day, time })
}

// Una hora del diario (LocalTime: "10:30" o "10:30:00") como la escribe un campo <input type="time">
export function hhmm(localTime: string): string {
  return localTime.slice(0, 5)
}

export function nowHhmm(now: Date = new Date()): string {
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}
