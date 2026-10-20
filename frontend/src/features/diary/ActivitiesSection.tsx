import { Button, Select, SelectItem, TextInput } from '@carbon/react'
import { Add } from '@carbon/icons-react'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import {
  ACTIVITY_TITLE_MAX_LENGTH,
  ACTIVITY_TYPES,
  ATTENDANCE_STATUSES,
  deleteActivity,
  recordAttendance,
  requiresTitle,
  rescheduleActivity,
  scheduleActivity,
  type Activity,
  type ActivityType,
  type AttendanceRow,
} from '../../api/diary'
import { todayIso } from '../../shared/dates'
import { hhmm } from '../../shared/times'
import { ConfirmDialog, ErrorNotice } from '../../shared/ui'
import { activityName, attendanceOf, byStartTime, summarize } from './activities'

interface Props {
  date: string
  activities: Activity[]
  onChanged: (activities: Activity[]) => void
}

// Las actividades del día y su lista. Cualquier empleado las programa (también con antelación) y pasa
// lista; "Rebajado" no se elige, sale de las rebajas del día
export function ActivitiesSection({ date, activities, onChanged }: Props) {
  const { t } = useTranslation()
  const [adding, setAdding] = useState(false)
  const [removing, setRemoving] = useState<Activity | null>(null)
  const [removeError, setRemoveError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  // Solo se pasa lista de hoy o de días anteriores
  const canTakeRoll = date <= todayIso()

  function replace(saved: Activity) {
    onChanged(byStartTime([...activities.filter((a) => a.id !== saved.id), saved]))
  }

  return (
    <section className="list-section" aria-labelledby="activities-title">
      <div className="toolbar">
        <h2 id="activities-title" className="toolbar__title">
          {t('diary.activities.title')}
        </h2>
        <p className="toolbar__count num">{t('diary.activities.count', { count: activities.length })}</p>
        {!adding && (
          <Button size="sm" kind="tertiary" renderIcon={Add} onClick={() => setAdding(true)}>
            {t('diary.activities.add')}
          </Button>
        )}
      </div>
      {!canTakeRoll && <p className="section-help">{t('diary.activities.future')}</p>}

      {adding && (
        <ActivityForm
          date={date}
          onSaved={(saved) => { replace(saved); setAdding(false) }}
          onCancel={() => setAdding(false)}
        />
      )}

      {activities.length === 0 ? (
        <p className="empty">{t('diary.activities.empty')}</p>
      ) : (
        <div className="activities">
          {activities.map((activity) => (
            <ActivityCard
              key={activity.id}
              date={date}
              activity={activity}
              canTakeRoll={canTakeRoll}
              onSaved={replace}
              onRemove={() => setRemoving(activity)}
            />
          ))}
        </div>
      )}

      <ConfirmDialog
        open={removing !== null}
        danger
        title={t('diary.activities.removeTitle', { name: removing ? activityName(removing, t) : '' })}
        confirmLabel={t('diary.activities.remove')}
        busy={busy}
        error={removeError}
        onClose={() => { setRemoving(null); setRemoveError(null) }}
        onConfirm={async () => {
          if (!removing) return
          setBusy(true)
          setRemoveError(null)
          try {
            await deleteActivity(removing.id)
            onChanged(activities.filter((a) => a.id !== removing.id))
            setRemoving(null)
          } catch (err) {
            setRemoveError(err)
          } finally {
            setBusy(false)
          }
        }}
      >
        <p>{t('diary.activities.removeBody')}</p>
      </ConfirmDialog>
    </section>
  )
}

interface FormProps {
  date: string
  // Sin actividad: se programa una nueva
  activity?: Activity
  onSaved: (activity: Activity) => void
  onCancel: () => void
}

function ActivityForm({ date, activity, onSaved, onCancel }: FormProps) {
  const { t } = useTranslation()
  const [startTime, setStartTime] = useState(activity ? hhmm(activity.startTime) : '')
  const [type, setType] = useState<ActivityType>(activity?.type ?? 'THERAPEUTIC_GROUP')
  const [title, setTitle] = useState(activity?.title ?? '')
  const [invalid, setInvalid] = useState<string | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const idPrefix = activity ? `activity-${activity.id}` : 'activity-new'
  const titleNeeded = requiresTitle(type)

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!startTime) return setInvalid(t('diary.activities.chooseTime'))
    if (titleNeeded && !title.trim()) return setInvalid(t(`diary.activities.titleNeeded.${type}`))
    setInvalid(null)
    setBusy(true)
    setError(null)
    try {
      onSaved(activity
        ? await rescheduleActivity(activity.id, startTime, type, title)
        : await scheduleActivity(date, startTime, type, title))
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="panel activity-form" onSubmit={save} noValidate>
      <ErrorNotice error={error} title={t('form.notSaved')} />
      <div className="activity-form__row">
        <TextInput id={`${idPrefix}-time`} type="time" labelText={t('diary.activities.time')} value={startTime}
          onChange={(e) => setStartTime(e.target.value)} />
        <Select id={`${idPrefix}-type`} labelText={t('diary.activities.typeLabel')} value={type}
          onChange={(e) => setType(e.target.value as ActivityType)}>
          {ACTIVITY_TYPES.map((value) => (
            <SelectItem key={value} value={value} text={t(`diary.activities.type.${value}`)} />
          ))}
        </Select>
      </div>
      <TextInput
        id={`${idPrefix}-title`}
        labelText={titleNeeded ? t(`diary.activities.titleLabel.${type}`) : t('diary.activities.titleOptional')}
        value={title}
        maxLength={ACTIVITY_TITLE_MAX_LENGTH}
        onChange={(e) => setTitle(e.target.value)}
      />
      {invalid && <p className="form-error" role="alert">{invalid}</p>}
      <div className="form-actions">
        <Button kind="secondary" size="sm" type="button" onClick={onCancel} disabled={busy}>
          {t('common.cancel')}
        </Button>
        <Button size="sm" type="submit" disabled={busy}>
          {busy ? t('common.saving') : t('common.save')}
        </Button>
      </div>
    </form>
  )
}

interface CardProps {
  date: string
  activity: Activity
  canTakeRoll: boolean
  onSaved: (activity: Activity) => void
  onRemove: () => void
}

function ActivityCard({ date, activity, canTakeRoll, onSaved, onRemove }: CardProps) {
  const { t } = useTranslation()
  const [mode, setMode] = useState<'view' | 'edit' | 'roll'>('view')
  const titleId = `activity-${activity.id}-title`
  const summary = summarize(activity.attendance)
  const recorded = summary.ATTENDED + summary.EXCUSED_ABSENCE + summary.UNEXCUSED_ABSENCE

  if (mode === 'edit') {
    return (
      <ActivityForm
        date={date}
        activity={activity}
        onSaved={(saved) => { onSaved(saved); setMode('view') }}
        onCancel={() => setMode('view')}
      />
    )
  }

  const counts = [
    ...ATTENDANCE_STATUSES.map((status) => summary[status] > 0 && t(`diary.attendance.count.${status}`, { count: summary[status] })),
    summary.reduced > 0 && t('diary.attendance.count.reduced', { count: summary.reduced }),
    recorded > 0 && summary.pending > 0 && t('diary.attendance.count.pending', { count: summary.pending }),
  ].filter(Boolean)

  return (
    <section className="panel activity" aria-labelledby={titleId}>
      <header className="activity__header">
        <span className="activity__time num">{hhmm(activity.startTime)}</span>
        <h3 id={titleId} className="activity__title">{activityName(activity, t)}</h3>
        {mode === 'view' && (
          <span className="row-actions activity__actions">
            {canTakeRoll && (
              <button type="button" className="text-action" onClick={() => setMode('roll')}>
                {recorded > 0 ? t('diary.attendance.edit') : t('diary.attendance.take')}
              </button>
            )}
            <button type="button" className="text-action" onClick={() => setMode('edit')}>
              {t('common.edit')}
            </button>
            <button type="button" className="text-action text-action--danger" onClick={onRemove}>
              {t('diary.activities.remove')}
            </button>
          </span>
        )}
      </header>

      {mode === 'roll' ? (
        <RollCall activity={activity} onSaved={(saved) => { onSaved(saved); setMode('view') }} onCancel={() => setMode('view')} />
      ) : (
        <p className="activity__summary">
          {recorded > 0 ? counts.join(' · ') : canTakeRoll ? t('diary.attendance.notTaken') : t('diary.attendance.notYet')}
          {recorded === 0 && summary.reduced > 0 && ` · ${t('diary.attendance.count.reduced', { count: summary.reduced })}`}
        </p>
      )}
    </section>
  )
}

// Pasar lista: un control por residente con las tres opciones; los rebajados aparecen sin control
function RollCall({ activity, onSaved, onCancel }: { activity: Activity; onSaved: (a: Activity) => void; onCancel: () => void }) {
  const { t } = useTranslation()
  const [rows, setRows] = useState<AttendanceRow[]>(activity.attendance)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const pending = rows.filter((r) => !r.reduced && !r.status).length

  function mark(id: string, status: AttendanceRow['status']) {
    setRows(rows.map((r) => (r.resident.id === id ? { ...r, status } : r)))
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      onSaved(await recordAttendance(activity.id, attendanceOf(rows)))
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  if (rows.length === 0) {
    return (
      <div className="roll-call">
        <p className="empty">{t('diary.attendance.nobody')}</p>
        <div className="form-actions">
          <Button kind="secondary" size="sm" type="button" onClick={onCancel}>{t('common.cancel')}</Button>
        </div>
      </div>
    )
  }

  return (
    <form className="roll-call" onSubmit={save} noValidate>
      <ErrorNotice error={error} title={t('form.notSaved')} />
      <div className="roll-call__tools">
        <p className="roll-call__pending">{t('diary.attendance.pending', { count: pending })}</p>
        <button type="button" className="text-action" disabled={pending === 0}
          onClick={() => setRows(rows.map((r) => (r.reduced || r.status ? r : { ...r, status: 'ATTENDED' })))}>
          {t('diary.attendance.markRest')}
        </button>
      </div>
      <ul className="roll-call__list">
        {rows.map((r) => {
          const name = `${r.resident.name} ${r.resident.surname}`
          const group = `roll-${activity.id}-${r.resident.id}`
          return (
            <li key={r.resident.id} className="roll-call__row">
              <span className="roll-call__number num">{r.resident.number ?? <span className="dash">—</span>}</span>
              <span className="roll-call__who">
                <span id={`${group}-name`}>{name}</span>
                {r.partialReduction && (
                  <span className="roll-call__note">{t('diary.attendance.partial', { partial: r.partialReduction })}</span>
                )}
              </span>
              {r.reduced ? (
                <span className="tag-reduced">{t('diary.attendance.reduced')}</span>
              ) : (
                <fieldset className="segmented" aria-labelledby={`${group}-name`}>
                  {ATTENDANCE_STATUSES.map((status) => (
                    <label key={status} className={`segmented__option segmented__option--${status}`}>
                      <input type="radio" name={group} value={status} checked={r.status === status}
                        onChange={() => mark(r.resident.id, status)} />
                      <span>{t(`diary.attendance.status.${status}`)}</span>
                    </label>
                  ))}
                </fieldset>
              )}
            </li>
          )
        })}
      </ul>
      <div className="form-actions">
        <Button kind="secondary" size="sm" type="button" onClick={onCancel} disabled={busy}>
          {t('common.cancel')}
        </Button>
        <Button size="sm" type="submit" disabled={busy}>
          {busy ? t('common.saving') : t('diary.attendance.save')}
        </Button>
      </div>
    </form>
  )
}
