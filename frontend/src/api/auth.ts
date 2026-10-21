import { request } from './http'
import type { LoginResponse } from './types'

export function login(email: string, password: string) {
  return request<LoginResponse>('/auth/login', {
    method: 'POST',
    body: { email, password },
    anonymous: true,
  })
}

// La pantalla del enlace del correo: se usa sin haber entrado, el token del enlace hace de credencial
export function checkPasswordSetup(token: string) {
  return request<{ name: string; email: string }>(`/auth/password-setup/${encodeURIComponent(token)}`, {
    anonymous: true,
  })
}

export function completePasswordSetup(token: string, newPassword: string) {
  return request<void>(`/auth/password-setup/${encodeURIComponent(token)}`, {
    method: 'POST',
    body: { newPassword },
    anonymous: true,
  })
}
