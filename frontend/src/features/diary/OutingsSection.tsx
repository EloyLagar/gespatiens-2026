import { Button, Select, SelectItem, TextInput } from '@carbon/react'
import { Add } from '@carbon/icons-react'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { deleteOuting, OUTING_NOTES_MAX_LENGTH, recordOuting, updateOuting, type Outing } from '../../api/diary'
import type { Patient } from '../../api/types'
import { todayIso } from '../../shared/dates'
import { hhmm, nowHhmm } from '../../shared/times'
import { ConfirmDialog, ErrorNotice } from '../../shared/ui'

interface Props {
  date: string
  outings: Outing[]
  // Residentes en el centro, para elegir quién sale
  residents: Patient[]
  onChanged: (outings: Outing[]) => void
}

interface Draft {
  // null: salida nueva
  outingId: string | null
  patientId: string
  leftAt: string
  returnedAt: string
  notes: string
}

function byLeftAt(list: Outing[]) {
  return [...list].sort((a, b) => a.leftAt.localeCompare(b.leftAt))
}

// La memoria: "las salidas terapéuticas y los regresos de estas permitiendo introducir la hora".
// Una fila por salida; cualquier empleado la anota, apunta el regreso o la corrige
export function OutingsSection({ date, outings, residents, onChanged }: Props) {
  const { t } = useTranslation()
  const [draft, setDraft] = useState<Draft | null>(null)
  const [invalid, setInvalid] = useState<string | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [removing, setRemoving] = useState<Outing | null>(null)
  const [removeError, setRemoveError] = useState<unknown>(null)
  // La hora actual solo tiene sentido como sugerencia en la página de hoy
  const suggestedTime = date === todayIso() ? nowHhmm() : ''
  const out = outings.filter((o) => !o.returnedAt).length

  function open(next: Draft) {
    setInvalid(null)
    setError(null)
    setDraft(next)
  }

  function edit(outing: Outing, returning = false) {
    open({
      outingId: outing.id,
      patientId: outing.resident.id,
      leftAt: hhmm(outing.leftAt),
      returnedAt: outing.returnedAt ? hhmm(outing.returnedAt) : returning ? suggestedTime : '',
      notes: outing.notes ?? '',
    })
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!draft) return
    if (!draft.patientId) return setInvalid(t('diary.outings.chooseResident'))
    if (!draft.leftAt) return setInvalid(t('diary.outings.chooseLeftAt'))
    if (draft.returnedAt && draft.returnedAt < draft.leftAt) return setInvalid(t('diary.outings.returnBeforeLeave'))
    setInvalid(null)
    setBusy(true)
    setError(null)
    try {
      const saved = draft.outingId
        ? await updateOuting(draft.outingId, draft.leftAt, draft.returnedAt || null, draft.notes)
        : await recordOuting(date, draft.patientId, draft.leftAt, draft.notes)
      onChanged(byLeftAt([...outings.filter((o) => o.id !== saved.id), saved]))
      setDraft(null)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  const editing = draft?.outingId ? outings.find((o) => o.id === draft.outingId) : undefined
  const times = (o: Outing) =>
    [t('diary.outings.leftAtShort', { time: hhmm(o.leftAt) }),
      o.returnedAt ? t('diary.outings.returnedAtShort', { time: hhmm(o.returnedAt) }) : t('diary.outings.away')].join(' · ')

  return (
    <section className="list-section" aria-labelledby="outings-title">
      <div className="toolbar">
        <h2 id="outings-title" className="toolbar__title">
          {t('diary.outings.title')}
        </h2>
        <p className="toolbar__count num">
          {t('diary.outings.count', { count: outings.length })}
          {out > 0 && ` · ${t('diary.outings.awayCount', { count: out })}`}
        </p>
        {!draft && (
          <Button size="sm" kind="tertiary" renderIcon={Add} disabled={residents.length === 0}
            onClick={() => open({ outingId: null, patientId: residents[0]?.id ?? '', leftAt: suggestedTime, returnedAt: '', notes: '' })}>
            {t('diary.outings.add')}
          </Button>
        )}
      </div>

      {draft && (
        <form className="panel outing-form" onSubmit={save} noValidate>
          <ErrorNotice error={error} title={t('form.notSaved')} />
          {editing ? (
            <p className="reduction-form__who">
              {editing.resident.number ?? '—'} · {editing.resident.name} {editing.resident.surname}
            </p>
          ) : (
            <Select id="outing-patient" labelText={t('diary.outings.resident')} value={draft.patientId}
              onChange={(e) => setDraft({ ...draft, patientId: e.target.value })}>
              {residents.map((p) => (
                <SelectItem key={p.id} value={p.id} text={`${p.number} · ${p.name} ${p.surname}`} />
              ))}
            </Select>
          )}
          <div className="activity-form__row">
            <TextInput id="outing-left" type="time" labelText={t('diary.outings.leftAt')} value={draft.leftAt}
              onChange={(e) => setDraft({ ...draft, leftAt: e.target.value })} />
            {editing && (
              <TextInput id="outing-returned" type="time" labelText={t('diary.outings.returnedAt')}
                helperText={t('diary.outings.returnedHelp')} value={draft.returnedAt}
                onChange={(e) => setDraft({ ...draft, returnedAt: e.target.value })} />
            )}
          </div>
          <TextInput id="outing-notes" labelText={t('diary.outings.notes')} helperText={t('diary.outings.notesHelp')}
            value={draft.notes} maxLength={OUTING_NOTES_MAX_LENGTH}
            onChange={(e) => setDraft({ ...draft, notes: e.target.value })} />
          {invalid && <p className="form-error" role="alert">{invalid}</p>}
          <div className="form-actions">
            <Button kind="secondary" size="sm" type="button" onClick={() => setDraft(null)} disabled={busy}>
              {t('common.cancel')}
            </Button>
            <Button size="sm" type="submit" disabled={busy}>
              {busy ? t('common.saving') : t('common.save')}
            </Button>
          </div>
        </form>
      )}

      {outings.length === 0 ? (
        <p className="empty">{t('diary.outings.empty')}</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table data-table--stack">
            <caption className="visually-hidden">{t('diary.outings.caption')}</caption>
            <thead>
              <tr>
                <th scope="col" className="num col-number">{t('patient.number')}</th>
                <th scope="col">{t('patient.fullName')}</th>
                <th scope="col" className="num">{t('diary.outings.leftAt')}</th>
                <th scope="col" className="num">{t('diary.outings.returnedAt')}</th>
                <th scope="col">{t('diary.outings.notes')}</th>
                <th scope="col" className="col-action"><span className="visually-hidden">{t('common.actions')}</span></th>
              </tr>
            </thead>
            <tbody>
              {outings.map((o) => (
                <tr key={o.id}>
                  <td className="num col-number">{o.resident.number ?? <span className="dash">—</span>}</td>
                  <td className="col-identity">
                    {o.resident.name} {o.resident.surname}
                    <span className="row-sub">{times(o)}{o.notes && ` · ${o.notes}`}</span>
                  </td>
                  <td className="num col-wide">{hhmm(o.leftAt)}</td>
                  <td className="num col-wide">
                    {o.returnedAt ? hhmm(o.returnedAt) : <span className="tag-away">{t('diary.outings.away')}</span>}
                  </td>
                  <td className="col-wide">{o.notes ?? <span className="dash">—</span>}</td>
                  <td className="col-action">
                    <span className="row-actions">
                      {!o.returnedAt && (
                        <button type="button" className="text-action" onClick={() => edit(o, true)}>
                          {t('diary.outings.markReturn')}
                        </button>
                      )}
                      <button type="button" className="text-action" onClick={() => edit(o)}>
                        {t('common.edit')}
                      </button>
                      <button type="button" className="text-action text-action--danger" onClick={() => setRemoving(o)}>
                        {t('diary.outings.remove')}
                      </button>
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={removing !== null}
        danger
        title={t('diary.outings.removeTitle', { name: removing ? `${removing.resident.name} ${removing.resident.surname}` : '' })}
        confirmLabel={t('diary.outings.remove')}
        busy={busy}
        error={removeError}
        onClose={() => { setRemoving(null); setRemoveError(null) }}
        onConfirm={async () => {
          if (!removing) return
          setBusy(true)
          setRemoveError(null)
          try {
            await deleteOuting(removing.id)
            onChanged(outings.filter((o) => o.id !== removing.id))
            setRemoving(null)
          } catch (err) {
            setRemoveError(err)
          } finally {
            setBusy(false)
          }
        }}
      >
        <p>{t('diary.outings.removeBody')}</p>
      </ConfirmDialog>
    </section>
  )
}
