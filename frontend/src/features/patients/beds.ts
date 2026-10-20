import { MAX_RESIDENTS, type Patient } from '../../api/types'

export interface Bed {
  number: number
  patient: Patient | null
}

// Las 32 plazas del centro con su residente, o vacías
export function bedsOf(activePatients: Patient[]): Bed[] {
  const byNumber = new Map(activePatients.filter((p) => p.number !== null).map((p) => [p.number as number, p]))
  return Array.from({ length: MAX_RESIDENTS }, (_, i) => ({
    number: i + 1,
    patient: byNumber.get(i + 1) ?? null,
  }))
}

// RF11: la próxima alta recibe el menor número libre. Es la misma regla que ResidentNumberAssigner
// en el dominio; aquí solo sirve para anunciarlo, quien asigna de verdad es la API
export function nextFreeNumber(beds: Bed[]): number | null {
  return beds.find((bed) => bed.patient === null)?.number ?? null
}
