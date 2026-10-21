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

// Algo que vence pronto o ya ha vencido. reportId solo en el informe; detail (el juzgado) solo en la medida
export interface DashboardDeadline {
  kind: 'MID_STAY_REPORT' | 'COURT_MEASURE_END'
  resident: { id: string; number: number | null; name: string; surname: string }
  dueDate: IsoDate
  // Negativo: vencido hace tantos días
  daysLeft: number
  overdue: boolean
  reportId: string | null
  detail: string | null
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
  // De lo más urgente a lo menos. No depende del mes: es lo pendiente de hoy
  deadlines: DashboardDeadline[]
}

export function getDashboard(month: string) {
  return request<Dashboard>(`/dashboard?month=${month}`)
}
