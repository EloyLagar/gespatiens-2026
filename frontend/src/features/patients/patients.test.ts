import { describe, expect, it } from 'vitest'
import type { Patient } from '../../api/types'
import { ageOn, daysBetween, formatDate } from '../../shared/dates'
import { bedsOf, nextFreeNumber } from './beds'
import { matches } from './search'

function patient(number: number, name = 'Juan', surname = 'García López', dni = '12345678Z'): Patient {
  return {
    id: `p${number}`,
    number,
    active: true,
    name,
    surname,
    dni,
    birthDate: '1990-05-01',
    address: 'C/ Mayor 1',
    phoneNumber: '600111222',
    sip: null,
    visitCode: null,
    belongings: null,
    abuseSubstances: null,
    extraInfo: null,
    entryDate: '2026-10-01',
    exitDate: null,
  }
}

describe('plazas (RF10, RF11)', () => {
  it('pinta siempre las 32 plazas, con su residente o vacías', () => {
    const beds = bedsOf([patient(1), patient(3)])
    expect(beds).toHaveLength(32)
    expect(beds[0].patient?.id).toBe('p1')
    expect(beds[1].patient).toBeNull()
  })

  it('la próxima alta recibe el menor número libre', () => {
    expect(nextFreeNumber(bedsOf([]))).toBe(1)
    expect(nextFreeNumber(bedsOf([patient(1), patient(2), patient(4)]))).toBe(3)
  })

  it('con las 32 plazas ocupadas no hay número libre', () => {
    const full = Array.from({ length: 32 }, (_, i) => patient(i + 1))
    expect(nextFreeNumber(bedsOf(full))).toBeNull()
  })
})

describe('búsqueda de residentes', () => {
  const lucia = patient(5, 'Lucía', 'Martínez Ruiz', '00000002W')

  it('ignora tildes y mayúsculas', () => {
    expect(matches(lucia, 'lucia martinez')).toBe(true)
  })

  it('busca por DNI y por número de plaza exacto', () => {
    expect(matches(lucia, '00000002')).toBe(true)
    expect(matches(lucia, '5')).toBe(true)
    expect(matches(patient(15), '5')).toBe(false)
  })
})

describe('fechas', () => {
  it('muestra las fechas de la API en el formato del idioma', () => {
    expect(formatDate('2026-10-20', 'es')).toBe('20/10/2026')
    expect(formatDate(null, 'es')).toBe('—')
  })

  it('calcula la edad teniendo en cuenta si ya ha sido el cumpleaños', () => {
    expect(ageOn('1990-05-01', '2026-04-30')).toBe(35)
    expect(ageOn('1990-05-01', '2026-05-01')).toBe(36)
  })

  it('cuenta los días de estancia incluyendo el de entrada', () => {
    expect(daysBetween('2026-10-20', '2026-10-20')).toBe(1)
    expect(daysBetween('2026-10-01', '2026-10-20')).toBe(20)
  })
})
