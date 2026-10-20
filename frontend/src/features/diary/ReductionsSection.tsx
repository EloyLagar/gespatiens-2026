import { Button, Checkbox, Select, SelectItem, TextInput } from '@carbon/react'
import { Add } from '@carbon/icons-react'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { deleteReduction, PARTIAL_REDUCTION_MAX_LENGTH, recordReduction, type Reduction } from '../../api/diary'
import type { Patient } from '../../api/types'
import { ConfirmDialog, ErrorNotice } from '../../shared/ui'

interface Props {
  date: string
  reductions: Reduction[]
  // Equipo médico (memoria) y administración (decisión del centro) registran las rebajas; el resto solo las ve
  canEdit: boolean
  residents: Patient[]
  onChanged: (reductions: Reduction[]) => void
}

interface Draft {
  patientId: string
  partial: string
  total: boolean
  sport: boolean
}

const EMPTY: Draft = { patientId: '', partial: '', total: false, sport: false }

export function ReductionsSection({ date, reductions, canEdit, residents, onChanged }: Props) {
  const { t } = useTranslation()
  const [draft, setDraft] = useState<Draft | null>(null)
  const [editingExisting, setEditingExisting] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [invalid, setInvalid] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [removing, setRemoving] = useState<Reduction | null>(null)
  const [removeError, setRemoveError] = useState<unknown>(null)

  const reducedIds = new Set(reductions.map((r) => r.resident.id))
  const available = residents.filter((p) => !reducedIds.has(p.id))

  function sortByNumber(list: Reduction[]) {
    return [...list].sort((a, b) => (a.resident.number ?? 99) - (b.resident.number ?? 99))
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!draft) return
    if (!draft.patientId) return setInvalid(t('diary.reductions.chooseResident'))
    if (!draft.partial.trim() && !draft.total && !draft.sport) return setInvalid(t('diary.reductions.chooseType'))
    setInvalid(null)
    setBusy(true)
    setError(null)
    try {
      const saved = await recordReduction(date, draft.patientId, draft.partial, draft.total, draft.sport)
      onChanged(sortByNumber([...reductions.filter((r) => r.resident.id !== saved.resident.id), saved]))
      setDraft(null)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  const types = (r: Reduction) =>
    [r.total && t('diary.reductions.total'), r.sport && t('diary.reductions.sport'), r.partial && `${t('diary.reductions.partial')}: ${r.partial}`]
      .filter(Boolean)
      .join(' · ')

  return (
    <section className="list-section" aria-labelledby="reductions-title">
      <div className="toolbar">
        <h2 id="reductions-title" className="toolbar__title">
          {t('diary.reductions.title')}
        </h2>
        <p className="toolbar__count num">{t('diary.reductions.count', { count: reductions.length })}</p>
        {canEdit && !draft && (
          <Button size="sm" kind="tertiary" renderIcon={Add} disabled={available.length === 0}
            onClick={() => { setEditingExisting(false); setDraft({ ...EMPTY, patientId: available[0]?.id ?? '' }) }}>
            {t('diary.reductions.add')}
          </Button>
        )}
      </div>
      {!canEdit && <p className="section-help">{t('diary.reductions.onlyMedical')}</p>}

      {draft && (
        <form className="panel reduction-form" onSubmit={save} noValidate>
          <ErrorNotice error={error} title={t('form.notSaved')} />
          {editingExisting ? (
            <p className="reduction-form__who">
              {(() => {
                const r = reductions.find((x) => x.resident.id === draft.patientId)
                return r ? `${r.resident.number ?? '—'} · ${r.resident.name} ${r.resident.surname}` : ''
              })()}
            </p>
          ) : (
            <Select id="reduction-patient" labelText={t('diary.reductions.resident')} value={draft.patientId}
              onChange={(e) => setDraft({ ...draft, patientId: e.target.value })}>
              {available.map((p) => (
                <SelectItem key={p.id} value={p.id} text={`${p.number} · ${p.name} ${p.surname}`} />
              ))}
            </Select>
          )}
          <fieldset className="reduction-form__types">
            <legend>{t('diary.reductions.type')}</legend>
            <Checkbox id="reduction-total" labelText={t('diary.reductions.total')} checked={draft.total}
              onChange={(_, { checked }) => setDraft({ ...draft, total: checked })} />
            <Checkbox id="reduction-sport" labelText={t('diary.reductions.sport')} checked={draft.sport}
              onChange={(_, { checked }) => setDraft({ ...draft, sport: checked })} />
          </fieldset>
          <TextInput id="reduction-partial" labelText={t('diary.reductions.partialLabel')}
            helperText={t('diary.reductions.partialHelp')} value={draft.partial} maxLength={PARTIAL_REDUCTION_MAX_LENGTH}
            onChange={(e) => setDraft({ ...draft, partial: e.target.value })} />
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

      {reductions.length === 0 ? (
        <p className="empty">{t('diary.reductions.empty')}</p>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <caption className="visually-hidden">{t('diary.reductions.caption')}</caption>
            <thead>
              <tr>
                <th scope="col" className="num col-number">{t('patient.number')}</th>
                <th scope="col">{t('patient.fullName')}</th>
                <th scope="col">{t('diary.reductions.type')}</th>
                {canEdit && <th scope="col" className="col-action"><span className="visually-hidden">{t('common.actions')}</span></th>}
              </tr>
            </thead>
            <tbody>
              {reductions.map((r) => (
                <tr key={r.resident.id}>
                  <td className="num col-number">{r.resident.number ?? <span className="dash">—</span>}</td>
                  <td className="col-identity">
                    {r.resident.name} {r.resident.surname}
                    <span className="row-sub">{types(r)}</span>
                  </td>
                  <td className="col-wide">{types(r)}</td>
                  {canEdit && (
                    <td className="col-action">
                      <span className="row-actions">
                        <button type="button" className="text-action" onClick={() => {
                          setEditingExisting(true)
                          setDraft({ patientId: r.resident.id, partial: r.partial ?? '', total: r.total, sport: r.sport })
                        }}>
                          {t('common.edit')}
                        </button>
                        <button type="button" className="text-action text-action--danger" onClick={() => setRemoving(r)}>
                          {t('diary.reductions.remove')}
                        </button>
                      </span>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={removing !== null}
        danger
        title={t('diary.reductions.removeTitle', { name: removing ? `${removing.resident.name} ${removing.resident.surname}` : '' })}
        confirmLabel={t('diary.reductions.remove')}
        busy={busy}
        error={removeError}
        onClose={() => { setRemoving(null); setRemoveError(null) }}
        onConfirm={async () => {
          if (!removing) return
          setBusy(true)
          setRemoveError(null)
          try {
            await deleteReduction(date, removing.resident.id)
            onChanged(reductions.filter((r) => r.resident.id !== removing.resident.id))
            setRemoving(null)
          } catch (err) {
            setRemoveError(err)
          } finally {
            setBusy(false)
          }
        }}
      >
        <p>{t('diary.reductions.removeBody')}</p>
      </ConfirmDialog>
    </section>
  )
}
