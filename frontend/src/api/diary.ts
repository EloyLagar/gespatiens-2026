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
  // RF8: la que se manda al guardarlo (0 si está sin rellenar)
  version: number
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

// Hora del día tal como la manda el backend (LocalTime): "10:30" o "10:30:00"
export type LocalTime = string

export const ACTIVITY_TYPES = [
  'THERAPEUTIC_GROUP',
  'LIFE_SKILLS',
  'HEALTH_SCHOOL',
  'JOB_GUIDANCE',
  'OCCUPATIONAL_WORKSHOP',
  'VIDEO_FORUM',
  'MAINTENANCE',
  'SPORT',
  'WALK',
  'OTHER',
] as const
export type ActivityType = (typeof ACTIVITY_TYPES)[number]

// Igual que ActivityType.requiresTitle(): el grupo terapéutico necesita su nombre; "Otros", qué es
export function requiresTitle(type: ActivityType) {
  return type === 'THERAPEUTIC_GROUP' || type === 'OTHER'
}

// "Rebajado" no se elige: sale de las rebajas del día
export const ATTENDANCE_STATUSES = ['ATTENDED', 'EXCUSED_ABSENCE', 'UNEXCUSED_ABSENCE'] as const
export type AttendanceStatus = (typeof ATTENDANCE_STATUSES)[number]

// RF6: la nota que pone el personal y la satisfacción y utilidad que da el residente, de 0 a 10 con un
// decimal. Lo que no se tenga va a null. También es la forma de las medias de una actividad
export const EVALUATION_FIELDS = ['mark', 'satisfaction', 'utility'] as const
export type EvaluationField = (typeof EVALUATION_FIELDS)[number]
export type Evaluation = Record<EvaluationField, number | null>

export interface AttendanceRow {
  resident: DiaryResident
  // null: sin anotar (o rebajado, si reduced)
  status: AttendanceStatus | null
  reduced: boolean
  partialReduction: string | null
  // null: sin valorar. Solo lo tienen quienes asistieron
  evaluation: Evaluation | null
}

export interface Activity {
  id: string
  startTime: LocalTime
  type: ActivityType
  title: string | null
  attendance: AttendanceRow[]
  averages: Evaluation
  // RF8: la que se manda al cambiarla, pasar lista o valorar
  version: number
}

export interface Outing {
  id: string
  resident: DiaryResident
  leftAt: LocalTime
  // null: todavía no ha vuelto
  returnedAt: LocalTime | null
  notes: string | null
}

export interface DiaryDay {
  date: IsoDate
  shifts: Shift[]
  reductions: Reduction[]
  activities: Activity[]
  outings: Outing[]
}

// Deben coincidir con ShiftLog.MAX_NOTES_LENGTH, Reduction.MAX_PARTIAL_LENGTH, Activity.MAX_TITLE_LENGTH
// y TherapeuticOuting.MAX_NOTES_LENGTH en el dominio
export const SHIFT_NOTES_MAX_LENGTH = 2000
export const PARTIAL_REDUCTION_MAX_LENGTH = 100
export const ACTIVITY_TITLE_MAX_LENGTH = 100
export const OUTING_NOTES_MAX_LENGTH = 200

export function getDiaryDay(date: IsoDate) {
  return request<DiaryDay>(`/diary/${date}`)
}

// version: la del turno en pantalla. Si otra persona lo guardó entretanto, 409
export function recordShift(date: IsoDate, part: DayPart, staffIds: string[], notes: string, version: number) {
  return request<Shift>(`/diary/${date}/shifts/${part}`, { method: 'PUT', body: { staffIds, notes, version } })
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

export function scheduleActivity(date: IsoDate, startTime: LocalTime, type: ActivityType, title: string) {
  return request<Activity>(`/diary/${date}/activities`, { method: 'POST', body: { startTime, type, title } })
}

export function rescheduleActivity(activityId: string, startTime: LocalTime, type: ActivityType, title: string,
                                   version: number) {
  return request<Activity>(`/diary/activities/${activityId}`, { method: 'PUT', body: { startTime, type, title, version } })
}

export function deleteActivity(activityId: string) {
  return request<void>(`/diary/activities/${activityId}`, { method: 'DELETE' })
}

// Sustituye la lista entera: quien no aparece queda sin anotar
export function recordAttendance(activityId: string, attendance: Record<string, AttendanceStatus>, version: number) {
  return request<Activity>(`/diary/activities/${activityId}/attendance`, { method: 'PUT', body: { attendance, version } })
}

// Sustituye todas las valoraciones: quien no aparece queda sin valorar. Solo educadores y administración
export function recordEvaluations(activityId: string, evaluations: Record<string, Evaluation>, version: number) {
  return request<Activity>(`/diary/activities/${activityId}/evaluations`, { method: 'PUT', body: { evaluations, version } })
}

export function recordOuting(date: IsoDate, patientId: string, leftAt: LocalTime, notes: string) {
  return request<Outing>(`/diary/${date}/outings`, { method: 'POST', body: { patientId, leftAt, notes } })
}

export function updateOuting(outingId: string, leftAt: LocalTime, returnedAt: LocalTime | null, notes: string) {
  return request<Outing>(`/diary/outings/${outingId}`, { method: 'PUT', body: { leftAt, returnedAt, notes } })
}

export function deleteOuting(outingId: string) {
  return request<void>(`/diary/outings/${outingId}`, { method: 'DELETE' })
}
