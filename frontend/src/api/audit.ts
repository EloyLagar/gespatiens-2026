import { request } from './http'
import type { IsoDate } from './types'

// AuditController en Spring. Solo la administración

// Solo cambios: las consultas no se registran
export type AuditAction =
  | 'ADMIT' | 'EDIT_RECORD' | 'DISCHARGE' | 'READMIT' | 'DELETE_RECORD'
  | 'EDIT_VISITORS' | 'EDIT_REFERRAL'
  | 'EDIT_REPORT' | 'CLOSE_REPORT' | 'REOPEN_REPORT'
  | 'EDIT_FOLLOW_UP' | 'ASSIGN_TUTORS'

export interface AuditEntry {
  id: string
  occurredAt: string
  action: AuditAction
  // name null: ya no está en la plantilla
  actor: { id: string; name: string | null }
  // null: sin residente. name null: la ficha se ha borrado
  resident: { id: string; number: number | null; name: string | null; surname: string | null } | null
  // El informe o la intervención concretos
  resourceId: string | null
}

export interface AuditPage {
  entries: AuditEntry[]
  page: number
  totalPages: number
  totalEntries: number
}

export interface AuditFilter {
  patientId?: string
  actorId?: string
  from?: IsoDate
  to?: IsoDate
  page?: number
}

export function searchAudit(filter: AuditFilter) {
  const params = new URLSearchParams()
  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  return request<AuditPage>(`/audit?${params}`)
}

// ---------------------------------------------------------------- Registro de accesos (AccessLogController)

// SUCCESS: entró. FAILURE: email o contraseña mal. LOCKED: el fallo que bloqueó el email. BLOCKED: lo intentó
// con el email bloqueado. PASSWORD_*: qué se hizo con su contraseña
export type AccessOutcome =
  | 'SUCCESS' | 'FAILURE' | 'LOCKED' | 'BLOCKED'
  | 'PASSWORD_CHANGED' | 'PASSWORD_CHANGE_REJECTED' | 'PASSWORD_SET' | 'PASSWORD_RESET'

export interface AccessEntry {
  id: string
  occurredAt: string
  // El que se escribió, exista o no
  email: string
  // null: el email no es de nadie. name null: ya no está en la plantilla
  employee: { id: string; name: string | null } | null
  // Quién lo hizo, si no fue el propio empleado: la administración que pone una contraseña provisional
  actor: { id: string; name: string | null } | null
  outcome: AccessOutcome
  ipAddress: string | null
  userAgent: string | null
}

export interface AccessPage {
  entries: AccessEntry[]
  page: number
  totalPages: number
  totalEntries: number
}

export interface AccessFilter {
  userId?: string
  outcomes?: AccessOutcome[]
  from?: IsoDate
  to?: IsoDate
  page?: number
}

export function searchAccessLog({ outcomes, ...filter }: AccessFilter) {
  const params = new URLSearchParams()
  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  outcomes?.forEach((o) => params.append('outcome', o))
  return request<AccessPage>(`/audit/access?${params}`)
}

// Lo que ve cada empleado al entrar (ENS op.acc). previous null: es su primera entrada registrada
export interface MyLastAccess {
  previous: { occurredAt: string; ipAddress: string | null; userAgent: string | null } | null
  failedAttemptsSince: number
}

export function getMyLastAccess() {
  return request<MyLastAccess>('/audit/access/mine')
}
