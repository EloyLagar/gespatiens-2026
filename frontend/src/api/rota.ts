import { request } from './http'
import type { IsoDate, Speciality } from './types'

// Tipos de RotaController en Spring. Si cambia un record de Java, esto cambia con él.
// El cuadrante es planificación: no hay fichajes ni se compara lo planificado con lo trabajado

export type ShiftPart = 'MORNING' | 'AFTERNOON' | 'NIGHT'
export const SHIFT_PARTS: ShiftPart[] = ['MORNING', 'AFTERNOON', 'NIGHT']

export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'
export const WEEKDAYS: Weekday[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

// yyyy-MM
export type IsoMonth = string

export interface RotaEmployee {
  id: string
  name: string
  speciality: Speciality
}

export interface PlannedShift {
  id: string
  date: IsoDate
  part: ShiftPart
  employeeId: string
}

export interface Rota {
  month: IsoMonth
  // Toda la plantilla, ordenada por especialidad y nombre
  employees: RotaEmployee[]
  shifts: PlannedShift[]
}

export type SwapStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED'

export interface ShiftSwap {
  id: string
  status: SwapStatus
  // null: la persona o el turno ya no existen
  requester: RotaEmployee | null
  partner: RotaEmployee | null
  requesterShift: PlannedShift | null
  // null también cuando el compañero solo cubre el turno, sin dar otro a cambio
  partnerShift: PlannedShift | null
  createdAt: string
  resolvedAt: string | null
}

export function getRota(month: IsoMonth) {
  return request<Rota>(`/rota?month=${month}`)
}

// ---- Planificar (la administración)

export function assignShift(date: IsoDate, part: ShiftPart, employeeId: string) {
  return request<PlannedShift>('/rota/shifts', { method: 'PUT', body: { date, part, employeeId } })
}

export function unassignShift(id: string) {
  return request<void>(`/rota/shifts/${id}`, { method: 'DELETE' })
}

// Devuelven cuántos turnos se han añadido o quitado
interface Changed {
  changed: number
}

export function fillRange(body: { employeeId: string; from: IsoDate; to: IsoDate; weekdays: Weekday[]; parts: ShiftPart[] }) {
  return request<Changed>('/rota/fill', { method: 'POST', body })
}

export function clearRange(body: { employeeId: string; from: IsoDate; to: IsoDate }) {
  return request<Changed>('/rota/clear', { method: 'POST', body })
}

export function repeatWeek(sourceMonday: IsoDate, until: IsoDate) {
  return request<Changed>('/rota/repeat-week', { method: 'POST', body: { sourceMonday, until } })
}

export function copyMonth(from: IsoMonth, to: IsoMonth) {
  return request<Changed>('/rota/copy-month', { method: 'POST', body: { from, to } })
}

// ---- Cambios de turno (entre trabajadores, sin pasar por la administración)

export function mySwaps() {
  return request<ShiftSwap[]>('/rota/swaps')
}

// partnerShiftId null: solo se pide que el compañero cubra el turno
export function proposeSwap(myShiftId: string, partnerId: string, partnerShiftId: string | null) {
  return request<ShiftSwap>('/rota/swaps', { method: 'POST', body: { myShiftId, partnerId, partnerShiftId } })
}

export function resolveSwap(id: string, action: 'accept' | 'decline' | 'cancel') {
  return request<ShiftSwap>(`/rota/swaps/${id}/${action}`, { method: 'POST' })
}
