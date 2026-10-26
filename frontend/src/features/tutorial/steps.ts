import type { Speciality } from '../../api/types'

// Un paso del tutorial de bienvenida. Con "to", el tutorial abre ese apartado mientras lo explica.
// Los textos están en tutorial.<especialidad>.<key> y, si no, en tutorial.common.<key>
export interface TutorialStep {
  key: string
  to?: string
}

const welcome: TutorialStep = { key: 'welcome' }
const notices: TutorialStep = { key: 'notices', to: '/avisos' }
const diary: TutorialStep = { key: 'diary', to: '/diario' }
const evaluations: TutorialStep = { key: 'evaluations', to: '/valoraciones' }
const interventions: TutorialStep = { key: 'interventions', to: '/intervenciones' }
const reports: TutorialStep = { key: 'reports', to: '/informes' }
const rota: TutorialStep = { key: 'rota', to: '/turnos' }
const residents: TutorialStep = { key: 'residents', to: '/residentes' }
const employees: TutorialStep = { key: 'employees', to: '/empleados' }
const dashboard: TutorialStep = { key: 'dashboard', to: '/panel' }
const audit: TutorialStep = { key: 'audit', to: '/registro' }
const profile: TutorialStep = { key: 'profile', to: '/perfil' }
const end: TutorialStep = { key: 'end' }

// Cada especialidad empieza por lo que más va a usar. El panel de control y el registro de
// cambios solo salen en el de la administración, que es la única que puede abrirlos
export const TUTORIAL_STEPS: Record<Speciality, TutorialStep[]> = {
  ADMINISTRATOR: [welcome, dashboard, residents, employees, interventions, reports, diary, rota, audit, notices, profile, end],
  SOCIAL_EDUCATOR: [welcome, notices, diary, evaluations, interventions, reports, rota, residents, profile, end],
  SOCIAL_WORKER: [welcome, notices, residents, reports, diary, rota, profile, end],
  PSYCHOLOGIST: [welcome, notices, interventions, reports, diary, rota, residents, profile, end],
  MEDICAL_TEAM: [welcome, notices, diary, reports, residents, rota, profile, end],
}
