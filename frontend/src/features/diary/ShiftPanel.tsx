import { Button, Checkbox, TextArea } from '@carbon/react'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { recordShift, SHIFT_NOTES_MAX_LENGTH, type Shift } from '../../api/diary'
import type { User } from '../../api/types'
import { ErrorNotice } from '../../shared/ui'
import { whenPosted } from '../../shared/times'
import { initialOf, tintFor } from '../users/people'

interface Props {
  date: string
  shift: Shift
  // Toda la plantilla, para elegir quién trabajó
  staff: User[]
  onSaved: (shift: Shift) => void
}

// Un turno del día: quién trabajó y la información de interés. Cualquier empleado puede rellenarlo
export function ShiftPanel({ date, shift, staff, onSaved }: Props) {
  const { t, i18n } = useTranslation()
  const [editing, setEditing] = useState(false)
  const [chosen, setChosen] = useState<Set<string>>(new Set())
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const titleId = `shift-${shift.part}`

  function startEditing() {
    setChosen(new Set(shift.staff.map((s) => s.id)))
    setNotes(shift.notes ?? '')
    setError(null)
    setEditing(true)
  }

  function toggle(id: string, checked: boolean) {
    const next = new Set(chosen)
    if (checked) next.add(id)
    else next.delete(id)
    setChosen(next)
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      onSaved(await recordShift(date, shift.part, [...chosen], notes))
      setEditing(false)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  // Educación social primero: son quienes cubren los turnos; el resto de la plantilla después
  const ordered = [...staff].sort(
    (a, b) =>
      Number(b.speciality === 'SOCIAL_EDUCATOR') - Number(a.speciality === 'SOCIAL_EDUCATOR') ||
      a.name.localeCompare(b.name),
  )

  return (
    <section className="panel shift" aria-labelledby={titleId}>
      <h3 id={titleId} className="shift__title">
        {t(`diary.part.${shift.part}`)}
      </h3>

      {editing ? (
        <form onSubmit={save} className="shift__form" noValidate>
          <ErrorNotice error={error} title={t('form.notSaved')} />
          <fieldset className="staff-picker">
            <legend>{t('diary.whoWorked')}</legend>
            {ordered.map((u) => (
              <Checkbox
                key={u.id}
                id={`${titleId}-${u.id}`}
                labelText={`${u.name} · ${t(`speciality.${u.speciality}`)}`}
                checked={chosen.has(u.id)}
                onChange={(_, { checked }) => toggle(u.id, checked)}
              />
            ))}
          </fieldset>
          <TextArea
            id={`${titleId}-notes`}
            labelText={t('diary.notes')}
            helperText={t('diary.notesHelp')}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={4}
            maxCount={SHIFT_NOTES_MAX_LENGTH}
            enableCounter
          />
          <div className="form-actions">
            <Button kind="secondary" size="sm" type="button" onClick={() => setEditing(false)} disabled={busy}>
              {t('common.cancel')}
            </Button>
            <Button size="sm" type="submit" disabled={busy}>
              {busy ? t('common.saving') : t('common.save')}
            </Button>
          </div>
        </form>
      ) : (
        <>
          {shift.staff.length > 0 ? (
            <ul className="shift__staff" aria-label={t('diary.whoWorked')}>
              {shift.staff.map((s) => (
                <li key={s.id} className="person">
                  <span className="avatar" style={{ background: tintFor(s.id) }} aria-hidden="true">
                    {initialOf(s.name)}
                  </span>
                  <span>{s.name}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="shift__empty">{shift.updatedAt ? t('diary.noStaff') : t('diary.notFilled')}</p>
          )}
          {shift.notes && <p className="shift__notes">{shift.notes}</p>}
          <footer className="shift__footer">
            {shift.updatedAt && (
              <p className="shift__meta">
                {t('diary.filledBy', {
                  name: shift.updatedBy?.name ?? t('notices.formerEmployee'),
                  when: whenPosted(shift.updatedAt, t, i18n.language),
                })}
              </p>
            )}
            <button type="button" className="text-action" onClick={startEditing}>
              {shift.updatedAt ? t('common.edit') : t('diary.fill')}
            </button>
          </footer>
        </>
      )}
    </section>
  )
}
