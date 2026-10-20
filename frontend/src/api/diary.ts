import { request } from './http'
import type { IsoDate, Speciality } from './types'

export type DayPart = 'MORNING' | 'AFTERNOON' | 'NIGHT'

export interface DiaryStaff {
  id: string
  name: string
  speciality: Speciality
}

export interface Shift {
  part: DayPart
  staff: DiaryStaff[]
  notes: string | null
  updatedBy: DiaryStaff | null
  // null si nadie ha rellenado todavía este turno
  updatedAt: string | null
}

export interface DiaryResident {
  id: string
  number: number | null
  name: string
  surname: string
}

export interface Reduction {
  resident: DiaryResident
  partial: string | null
  total: boolean
  sport: boolean
}

export interface DiaryDay {
  date: IsoDate
  shifts: Shift[]
  reductions: Reduction[]
}

// Deben coincidir con ShiftLog.MAX_NOTES_LENGTH y Reduction.MAX_PARTIAL_LENGTH en el dominio
export const SHIFT_NOTES_MAX_LENGTH = 2000
export const PARTIAL_REDUCTION_MAX_LENGTH = 100

export function getDiaryDay(date: IsoDate) {
  return request<DiaryDay>(`/diary/${date}`)
}

export function recordShift(date: IsoDate, part: DayPart, staffIds: string[], notes: string) {
  return request<Shift>(`/diary/${date}/shifts/${part}`, { method: 'PUT', body: { staffIds, notes } })
}

export function recordReduction(date: IsoDate, patientId: string, partial: string, total: boolean, sport: boolean) {
  return request<Reduction>(`/diary/${date}/reductions/${patientId}`, {
    method: 'PUT',
    body: { partial, total, sport },
  })
}

export function deleteReduction(date: IsoDate, patientId: string) {
  return request<void>(`/diary/${date}/reductions/${patientId}`, { method: 'DELETE' })
}
