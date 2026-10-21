import { request } from './http'
import type { IsoDate, Patient, PatientDetails, PatientStatus } from './types'

export function listPatients(status: PatientStatus) {
  return request<Patient[]>(`/patients?status=${status}`)
}

export function getPatient(id: string) {
  return request<Patient>(`/patients/${id}`)
}

export function admitPatient(details: PatientDetails, entryDate: IsoDate) {
  return request<Patient>('/patients', { method: 'POST', body: { ...details, entryDate } })
}

export function updatePatient(id: string, details: PatientDetails) {
  return request<Patient>(`/patients/${id}`, { method: 'PUT', body: details })
}

export function dischargePatient(id: string, exitDate: IsoDate) {
  return request<Patient>(`/patients/${id}/discharge`, { method: 'POST', body: { exitDate } })
}

export function readmitPatient(id: string, entryDate: IsoDate) {
  return request<Patient>(`/patients/${id}/readmit`, { method: 'POST', body: { entryDate } })
}

export function deletePatient(id: string) {
  return request<void>(`/patients/${id}`, { method: 'DELETE' })
}

// Personas autorizadas a visitar a un residente (memoria final). Las ve todo el personal; las gestiona la
// administración
export interface Visitor {
  id: string
  name: string
  relationship: string
  phoneNumber: string
}

export type VisitorInput = Omit<Visitor, 'id'>

export function listVisitors(patientId: string) {
  return request<Visitor[]>(`/patients/${patientId}/visitors`)
}

export function authorizeVisitor(patientId: string, visitor: VisitorInput) {
  return request<Visitor>(`/patients/${patientId}/visitors`, { method: 'POST', body: visitor })
}

export function updateVisitor(patientId: string, visitorId: string, visitor: VisitorInput) {
  return request<Visitor>(`/patients/${patientId}/visitors/${visitorId}`, { method: 'PUT', body: visitor })
}

export function revokeVisitor(patientId: string, visitorId: string) {
  return request<void>(`/patients/${patientId}/visitors/${visitorId}`, { method: 'DELETE' })
}

// La derivación de un residente (memoria final, 5.2): quién lo deriva y, si la hay, la medida judicial
export interface Referral {
  referringInstitution: string | null
  contactName: string | null
  contactPhone: string | null
  courtOrdered: boolean
  // Solo si courtOrdered
  court: string | null
  measureEndDate: IsoDate | null
  notes: string | null
  updatedAt: string
}

export type ReferralInput = Omit<Referral, 'updatedAt'>

// Debe coincidir con Referral.MAX_NOTES_LENGTH en el dominio
export const REFERRAL_NOTES_MAX_LENGTH = 2000

export function getReferral(patientId: string) {
  return request<Referral>(`/patients/${patientId}/referral`)
}

export function updateReferral(patientId: string, referral: ReferralInput) {
  return request<Referral>(`/patients/${patientId}/referral`, { method: 'PUT', body: referral })
}
