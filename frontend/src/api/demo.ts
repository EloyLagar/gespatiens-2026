import { request } from './http'
import type { Speciality } from './types'

// DemoAccountsController: solo existe en la demo. En un centro de verdad la API responde con error y el login
// no enseña nada
export interface DemoAccount {
  name: string
  email: string
  speciality: Speciality
  password: string
}

export function demoAccounts() {
  return request<DemoAccount[]>('/demo/accounts', { anonymous: true })
}
