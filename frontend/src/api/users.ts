import { request } from './http'
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
