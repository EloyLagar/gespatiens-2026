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
