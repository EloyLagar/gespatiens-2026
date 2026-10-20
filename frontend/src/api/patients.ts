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
