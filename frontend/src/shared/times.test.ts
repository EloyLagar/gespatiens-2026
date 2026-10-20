import { describe, expect, it } from 'vitest'
import { whenPosted } from './times'

// Traducción falsa que deja ver la clave y sus valores
const t = (key: string, options?: Record<string, unknown>) =>
  options ? `${key} ${JSON.stringify(options)}` : key

describe('whenPosted', () => {
  const now = new Date(2026, 9, 20, 18, 0)

  it('dice "ahora mismo" en el primer minuto', () => {
    expect(whenPosted(new Date(2026, 9, 20, 17, 59, 30).toISOString(), t, 'es', now)).toBe('time.justNow')
  })

  it('cuenta los minutos durante la primera hora', () => {
    expect(whenPosted(new Date(2026, 9, 20, 17, 55).toISOString(), t, 'es', now)).toBe('time.minutesAgo {"count":5}')
  })

  it('da la hora si es de hoy o de ayer', () => {
    expect(whenPosted(new Date(2026, 9, 20, 9, 30).toISOString(), t, 'es', now)).toBe('time.today {"time":"09:30"}')
    expect(whenPosted(new Date(2026, 9, 19, 18, 10).toISOString(), t, 'es', now)).toBe('time.yesterday {"time":"18:10"}')
  })

  it('da la fecha completa si es más antiguo', () => {
    expect(whenPosted(new Date(2026, 9, 12, 8, 5).toISOString(), t, 'es', now)).toBe(
      'time.onDay {"day":"12/10/2026","time":"08:05"}',
    )
  })
})
