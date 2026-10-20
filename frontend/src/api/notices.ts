import { request } from './http'
import type { Notice } from './types'

export function listNotices() {
  return request<Notice[]>('/notices')
}

export function publishNotice(text: string) {
  return request<Notice>('/notices', { method: 'POST', body: { text } })
}

export function editNotice(id: string, text: string) {
  return request<Notice>(`/notices/${id}`, { method: 'PUT', body: { text } })
}

export function deleteNotice(id: string) {
  return request<void>(`/notices/${id}`, { method: 'DELETE' })
}
