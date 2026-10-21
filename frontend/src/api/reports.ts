import { ApiError, download, request } from './http'
import type { IsoDate, Speciality } from './types'

// Tipos de ReportResponses en Spring. Si cambia un record de Java, esto cambia con él

export type ReportType = 'MID_STAY' | 'FINAL'
export const REPORT_TYPES: ReportType[] = ['MID_STAY', 'FINAL']

// RF8: en elaboración se redacta; cerrado es el documento entregado y no cambia hasta que se reabra
export type ReportState = 'DRAFT' | 'CLOSED'

export type ReportArea = 'SOCIAL' | 'HEALTH' | 'PSYCHOLOGICAL' | 'EDUCATIONAL' | 'FAMILY' | 'DURATION' | 'FOLLOW_UP'

// El nombre de cada apartado (ReportSection en el dominio). Sus textos están en i18n: reports.sections
export type ReportSectionCode = string

export interface ReportStaff {
  id: string
  name: string
  speciality: Speciality
}

// Los datos de la ficha que van en la cabecera del informe. number null: ya no está en el centro
export interface ReportResident {
  id: string
  number: number | null
  name: string
  surname: string
  dni: string
  birthDate: IsoDate
  sip: string | null
  active: boolean
}

export interface ReportSummary {
  id: string
  type: ReportType
  state: ReportState
  resident: ReportResident
  // La estancia del informe. stayEnd null: sigue en el centro
  stayStart: IsoDate
  stayEnd: IsoDate | null
  // El último apartado guardado o, si no hay ninguno, cuándo se creó
  lastUpdate: string
  writtenSections: number
  totalSections: number
}

// Uno de cada tipo por estancia, que se crean solos al entrar. De la estancia más antigua a la más reciente
export interface ResidentReports {
  resident: ReportResident
  reports: ReportSummary[]
}

// Las fechas salen de la ficha; lo demás se escribe a mano
export interface ReportHeader {
  requestNumber: string | null
  // Por defecto "Comunidad Terapéutica Los Vientos"
  referenceCenter: string | null
  // Solo en el de media estancia
  nip: string | null
  entryDate: IsoDate | null
  // Solo en el final. null mientras sigue en el centro
  exitDate: IsoDate | null
  exitReason: string | null
  version: number
}

export interface ReportSection {
  section: ReportSectionCode
  area: ReportArea
  // Va dentro del "Grado de adaptación al centro" del área educativa
  adaptation: boolean
  // null y version 0: vacío
  text: string | null
  version: number
  // null: vacío, o quien lo escribió ya no está en la plantilla
  author: ReportStaff | null
  updatedAt: string | null
}

export interface ReportAreaInfo {
  area: ReportArea
  // Las especialidades que la redactan (la administración, todas)
  writers: Speciality[]
  // También (o solo) los tutores del residente
  tutors: boolean
  // Quien mira puede redactarla
  writable: boolean
}

export interface Report {
  id: string
  type: ReportType
  state: ReportState
  resident: ReportResident
  stayStart: IsoDate
  header: ReportHeader
  areas: ReportAreaInfo[]
  // Todos los del tipo de informe, en orden, escritos o no
  sections: ReportSection[]
  // "El presente informe ha sido elaborado por los siguientes profesionales"
  authors: ReportStaff[]
  // null: lo creó la aplicación al empezar la estancia
  createdBy: ReportStaff | null
  createdAt: string
  closedBy: ReportStaff | null
  closedAt: string | null
  writtenSections: number
  totalSections: number
  // Puede cerrarlo y reabrirlo: la administración
  manageable: boolean
}

// Lo que se escribe a mano en la cabecera
export interface ReportHeaderInput {
  requestNumber: string | null
  referenceCenter: string | null
  nip: string | null
  exitReason: string | null
}

// Debe coincidir con SectionEntry.MAX_LENGTH en el dominio
export const SECTION_MAX_LENGTH = 4000

export function listReports() {
  return request<ReportSummary[]>('/reports')
}

export function getResidentReports(patientId: string) {
  return request<ResidentReports>(`/reports/residents/${patientId}`)
}

export function getReport(id: string) {
  return request<Report>(`/reports/${id}`)
}

// version: la que tenía el apartado cuando se empezó a editar. Si otra persona lo ha guardado entretanto,
// la API responde 409 con lo que hay ahora (ver sectionConflictOf)
export function writeSection(id: string, section: ReportSectionCode, text: string, version: number) {
  return request<Report>(`/reports/${id}/sections/${section}`, { method: 'PUT', body: { text, version } })
}

export function editReportHeader(id: string, header: ReportHeaderInput, version: number) {
  return request<Report>(`/reports/${id}/header`, { method: 'PUT', body: { ...header, version } })
}

export function closeReport(id: string) {
  return request<Report>(`/reports/${id}/close`, { method: 'POST' })
}

export function reopenReport(id: string) {
  return request<Report>(`/reports/${id}/reopen`, { method: 'POST' })
}

// inline: para la vista previa en otra pestaña
export function reportPdf(id: string, inline = false) {
  return download(`/reports/${id}/pdf?inline=${inline}`)
}

// RF2: todos los informes de un residente en un único PDF; los de un tipo, o los dos (type null)
export function residentReportsPdf(patientId: string, type: ReportType | null, inline = false) {
  const query = new URLSearchParams({ inline: String(inline) })
  if (type) query.set('type', type)
  return download(`/reports/residents/${patientId}/pdf?${query}`)
}

export interface SectionConflict {
  // null: la otra persona lo dejó vacío
  currentText: string | null
  currentVersion: number
}

// RF8: el 409 de un apartado que otra persona ha modificado trae lo que hay ahora
export function sectionConflictOf(error: unknown): SectionConflict | null {
  if (!(error instanceof ApiError) || error.status !== 409 || typeof error.problem.section !== 'string') {
    return null
  }
  const { currentText, currentVersion } = error.problem
  return {
    currentText: typeof currentText === 'string' ? currentText : null,
    currentVersion: typeof currentVersion === 'number' ? currentVersion : 0,
  }
}
