import { request } from './http'
import type { LoginResponse } from './types'

export function login(email: string, password: string) {
  return request<LoginResponse>('/auth/login', {
    method: 'POST',
    body: { email, password },
    anonymous: true,
  })
}
