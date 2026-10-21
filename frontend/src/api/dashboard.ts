import { request } from './http'
import type { IsoDate, Speciality } from './types'

// DashboardController.DashboardResponse en Spring. Solo la administración

export interface DashboardShifts {
  employeeId: string
  name: string
  speciality: Speciality
  morning: number
  afternoon: number
  night: number
  total: number
}

export interface Dashboard {
  // yyyy-MM
  month: string
  // Las de ahora mismo
  occupiedPlaces: number
  totalPlaces: number
  residentsWithoutBothTutors: number
  reportsInProgress: number
  // Las del mes
  interventions: number
  // De quien más turnos hizo a quien menos
  shifts: DashboardShifts[]
  // Semanas completas (de lunes a domingo) que tocan el mes
  noticesByWeek: { monday: IsoDate; notices: number }[]
}

export function getDashboard(month: string) {
  return request<Dashboard>(`/dashboard?month=${month}`)
}
