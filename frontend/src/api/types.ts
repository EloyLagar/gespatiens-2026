// Tipos de los DTO de la API de Spring (infrastructure/web/*Response y *Request).
// Si cambia un record de Java, este fichero cambia con él en el mismo commit

export type Speciality =
  | 'ADMINISTRATOR'
  | 'SOCIAL_EDUCATOR'
  | 'SOCIAL_WORKER'
  | 'PSYCHOLOGIST'
  | 'MEDICAL_TEAM'

export const SPECIALITIES: Speciality[] = [
  'ADMINISTRATOR',
  'SOCIAL_EDUCATOR',
  'SOCIAL_WORKER',
  'PSYCHOLOGIST',
  'MEDICAL_TEAM',
]

export interface User {
  id: string
  name: string
  email: string
  phoneNumber: string
  speciality: Speciality
  // RNF9: el idioma que ha elegido. Puede faltar en una sesión guardada antes de existir
  language?: 'ES' | 'EN'
}

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: string
  user: User
}

// Fechas siempre en formato ISO yyyy-MM-dd, como las devuelve y espera la API
export type IsoDate = string

export interface Patient {
  id: string
  number: number | null
  active: boolean
  name: string
  surname: string
  dni: string
  birthDate: IsoDate
  address: string
  phoneNumber: string
  sip: string | null
  visitCode: string | null
  belongings: string | null
  abuseSubstances: string | null
  extraInfo: string | null
  entryDate: IsoDate
  exitDate: IsoDate | null
}

// La ficha editable: lo que envían el alta (POST) y la edición (PUT)
export interface PatientDetails {
  name: string
  surname: string
  dni: string
  birthDate: IsoDate
  address: string
  phoneNumber: string
  sip: string
  visitCode: string
  belongings: string
  abuseSubstances: string
  extraInfo: string
}

export type PatientStatus = 'ACTIVE' | 'FORMER'

// Debe coincidir con Patient.MAX_RESIDENTS en el dominio (RF10)
export const MAX_RESIDENTS = 32

export interface NoticeAuthor {
  id: string
  name: string
  speciality: Speciality
  administrator: boolean
}

export interface Notice {
  id: string
  text: string
  // Instantes ISO (UTC), p. ej. 2026-10-20T08:00:00Z
  createdAt: string
  editedAt: string | null
  // null si el autor ya no está en la plantilla
  author: NoticeAuthor | null
  mine: boolean
  manageable: boolean
}

// Debe coincidir con Notice.MAX_LENGTH en el dominio
export const NOTICE_MAX_LENGTH = 2000
