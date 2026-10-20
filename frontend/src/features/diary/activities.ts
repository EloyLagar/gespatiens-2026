import type { Activity, AttendanceRow, AttendanceStatus } from '../../api/diary'
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

export function byStartTime(activities: Activity[]): Activity[] {
  return [...activities].sort((a, b) => a.startTime.localeCompare(b.startTime))
}
