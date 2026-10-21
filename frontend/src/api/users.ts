import { ApiError, download, request } from './http'
import type { Speciality, User } from './types'

export function listUsers() {
  return request<User[]>('/users')
}

export function getUser(id: string) {
  return request<User>(`/users/${id}`)
}

export function getMe() {
  return request<User>('/users/me')
}

export interface NewUser {
  name: string
  email: string
  phoneNumber: string
  speciality: Speciality
  password: string
}

export function registerUser(user: NewUser) {
  return request<User>('/users', { method: 'POST', body: user })
}

export interface EditedUser {
  name: string
  email: string
  phoneNumber: string
  speciality: Speciality
}

export function editUser(id: string, user: EditedUser) {
  return request<User>(`/users/${id}`, { method: 'PUT', body: user })
}

export function resetUserPassword(id: string, newPassword: string) {
  return request<void>(`/users/${id}/password`, { method: 'PUT', body: { newPassword } })
}

export function deleteUser(id: string) {
  return request<void>(`/users/${id}`, { method: 'DELETE' })
}

export function updateMe(name: string, phoneNumber: string) {
  return request<User>('/users/me', { method: 'PUT', body: { name, phoneNumber } })
}

export function changeMyPassword(currentPassword: string, newPassword: string) {
  return request<void>('/users/me/password', { method: 'PUT', body: { currentPassword, newPassword } })
}

// La firma escaneada que se imprime en los informes. null: todavía no la ha subido
export async function getMySignature(): Promise<Blob | null> {
  try {
    return (await download('/users/me/signature', 'image/png')).blob
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) return null
    throw err
  }
}

export function uploadMySignature(png: Blob) {
  return request<void>('/users/me/signature', { method: 'PUT', file: png })
}

export function removeMySignature() {
  return request<void>('/users/me/signature', { method: 'DELETE' })
}

// RNF9: el idioma se guarda en el perfil, así el empleado lo encuentra igual en cualquier equipo
export function changeMyLanguage(language: 'ES' | 'EN') {
  return request<User>('/users/me/language', { method: 'PUT', body: { language } })
}
