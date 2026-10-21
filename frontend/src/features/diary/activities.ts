import {
  EVALUATION_FIELDS,
  type Activity,
  type AttendanceRow,
  type AttendanceStatus,
  type Evaluation,
  type EvaluationField,
} from '../../api/diary'
import type { Translate } from '../../shared/times'

// Cómo se nombra una actividad: el tipo y, si lo tiene, su nombre ("Grupo terapéutico · Autoestima").
// En "Otros" el nombre ya dice qué es
export function activityName(activity: Pick<Activity, 'type' | 'title'>, t: Translate): string {
  if (activity.type === 'OTHER' && activity.title) return activity.title
  const type = t(`diary.activities.type.${activity.type}`)
  return activity.title ? `${type} · ${activity.title}` : type
}

export interface AttendanceSummary {
  ATTENDED: number
  EXCUSED_ABSENCE: number
  UNEXCUSED_ABSENCE: number
  reduced: number
  pending: number
}

export function summarize(rows: AttendanceRow[]): AttendanceSummary {
  const summary: AttendanceSummary = { ATTENDED: 0, EXCUSED_ABSENCE: 0, UNEXCUSED_ABSENCE: 0, reduced: 0, pending: 0 }
  for (const row of rows) {
    if (row.reduced) summary.reduced++
    else if (row.status) summary[row.status]++
    else summary.pending++
  }
  return summary
}

// Lo anotado en la lista, tal como lo espera recordAttendance. Los rebajados no se mandan
export function attendanceOf(rows: AttendanceRow[]): Record<string, AttendanceStatus> {
  const entries: Record<string, AttendanceStatus> = {}
  for (const row of rows) {
    if (!row.reduced && row.status) entries[row.resident.id] = row.status
  }
  return entries
}

// Una puntuación tal como la escribe el personal: de 0 a 10 con un decimal, con coma o con punto.
// Vacío -> null; algo que no es una puntuación válida -> undefined
export function parseScore(text: string): number | null | undefined {
  const cleaned = text.trim().replace(',', '.')
  if (cleaned === '') return null
  if (!/^\d{1,2}(\.\d)?$/.test(cleaned)) return undefined
  const value = Number(cleaned)
  return value <= 10 ? value : undefined
}

export function formatScore(value: number | null, locale: string): string {
  if (value === null) return ''
  return new Intl.NumberFormat(locale === 'en' ? 'en-GB' : 'es-ES', { maximumFractionDigits: 1 }).format(value)
}

export type ScoreDraft = Record<EvaluationField, string>

export function draftOf(evaluation: Evaluation | null, locale: string): ScoreDraft {
  return {
    mark: formatScore(evaluation?.mark ?? null, locale),
    satisfaction: formatScore(evaluation?.satisfaction ?? null, locale),
    utility: formatScore(evaluation?.utility ?? null, locale),
  }
}

// Lo escrito para cada asistente, tal como lo espera recordEvaluations. Quien lo tiene todo vacío no se manda.
// null si alguna puntuación no es válida
export function evaluationsOf(drafts: Record<string, ScoreDraft>): Record<string, Evaluation> | null {
  const entries: Record<string, Evaluation> = {}
  for (const [patientId, draft] of Object.entries(drafts)) {
    const evaluation = {} as Evaluation
    for (const field of EVALUATION_FIELDS) {
      const value = parseScore(draft[field])
      if (value === undefined) return null
      evaluation[field] = value
    }
    if (EVALUATION_FIELDS.some((field) => evaluation[field] !== null)) entries[patientId] = evaluation
  }
  return entries
}

export function hasAverages(averages: Evaluation): boolean {
  return EVALUATION_FIELDS.some((field) => averages[field] !== null)
}

export function byStartTime(activities: Activity[]): Activity[] {
  return [...activities].sort((a, b) => a.startTime.localeCompare(b.startTime))
}
