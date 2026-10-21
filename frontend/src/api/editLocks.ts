import { ApiError, request } from './http'

// RF8: quién está editando qué. Tipos de EditLockController.EditLockResponse en Spring

export interface EditLock {
  resource: string
  holderId: string
  // null: quien lo edita ya no está en la plantilla
  holderName: string | null
  since: string
  expiresAt: string
  mine: boolean
}

// La pantalla lo renueva cada 30 s mientras se edita; si deja de hacerlo, caduca solo a los 90 s
export const LOCK_RENEW_MS = 30_000

const path = (resource: string) => `/edit-locks/${encodeURIComponent(resource)}`

export function acquireLock(resource: string) {
  return request<EditLock>(path(resource), { method: 'PUT' })
}

// keepalive: también al cerrar la pestaña
export function releaseLock(resource: string, keepalive = false) {
  return request<void>(path(resource), { method: 'DELETE', keepalive })
}

// Todo lo que se está editando dentro de algo, p. ej. "report:<id>:"
export function listLocks(prefix: string) {
  return request<EditLock[]>(`/edit-locks?prefix=${encodeURIComponent(prefix)}`)
}

// El 409 de "lo está editando otra persona" (al pedir el bloqueo o al guardar) trae su nombre
export function beingEditedBy(error: unknown): { name: string | null } | null {
  if (!(error instanceof ApiError) || error.status !== 409 || !('beingEditedBy' in error.problem)) {
    return null
  }
  const name = error.problem.beingEditedBy
  return { name: typeof name === 'string' ? name : null }
}

// Los nombres de lo que se bloquea dentro de un informe. Deben coincidir con EditingPresence en Spring
export const reportResources = {
  prefix: (reportId: string) => `report:${reportId}:`,
  section: (reportId: string, section: string) => `report:${reportId}:section:${section}`,
  header: (reportId: string) => `report:${reportId}:header`,
}
