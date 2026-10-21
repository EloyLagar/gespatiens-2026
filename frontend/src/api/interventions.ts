import { request } from './http'
import type { IsoDate, Speciality } from './types'

// Tipos de InterventionResponses en Spring. Si cambia un record de Java, esto cambia con él

export interface FollowUpStaff {
  id: string
  name: string
  speciality: Speciality
}

export interface FollowUpResident {
  id: string
  // null: ya no está en el centro
  number: number | null
  name: string
  surname: string
  active: boolean
}

// Un residente en el listado de "Intervenciones"
export interface FollowUpSummary {
  resident: FollowUpResident
  tutors: FollowUpStaff[]
  interventionCount: number
  // null: todavía no tiene ninguna
  lastIntervention: IsoDate | null
  // Quien mira es tutor suyo
  mine: boolean
}

export interface Intervention {
  id: string
  date: IsoDate
  text: string
  createdAt: string
  editedAt: string | null
  // null: el empleado ya no está en la plantilla
  author: FollowUpStaff | null
  mine: boolean
  // Puede modificarla y eliminarla: su autor o la administración
  manageable: boolean
}

export interface FollowUp {
  resident: FollowUpResident
  tutors: FollowUpStaff[]
  // De la más reciente a la más antigua
  interventions: Intervention[]
  // Quien mira es tutor del residente
  tutor: boolean
  // Es tutor y el residente sigue en el centro: puede registrar intervenciones
  canRecord: boolean
}

// Debe coincidir con Intervention.MAX_LENGTH en el dominio
export const INTERVENTION_MAX_LENGTH = 2000

export function listFollowUps() {
  return request<FollowUpSummary[]>('/interventions')
}

export function getFollowUp(patientId: string) {
  return request<FollowUp>(`/interventions/residents/${patientId}`)
}

// Los dos tutores: un educador social y un psicólogo (null: sin asignar). La administración y los educadores
export function assignTutors(patientId: string, educatorId: string | null, psychologistId: string | null) {
  return request<FollowUpStaff[]>(`/interventions/residents/${patientId}/tutors`, {
    method: 'PUT',
    body: { educatorId, psychologistId },
  })
}

// Solo los tutores del residente
export function recordIntervention(patientId: string, date: IsoDate, text: string) {
  return request<Intervention>(`/interventions/residents/${patientId}`, { method: 'POST', body: { date, text } })
}

export function editIntervention(id: string, date: IsoDate, text: string) {
  return request<Intervention>(`/interventions/${id}`, { method: 'PUT', body: { date, text } })
}

export function deleteIntervention(id: string) {
  return request<void>(`/interventions/${id}`, { method: 'DELETE' })
}
