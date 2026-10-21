import { Button, Checkbox, TextArea, TextInput } from '@carbon/react'
import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { getReferral, REFERRAL_NOTES_MAX_LENGTH, updateReferral, type Referral, type ReferralInput } from '../../api/patients'
import { formatDate } from '../../shared/dates'
import { ErrorNotice, Field, LoadingRows } from '../../shared/ui'
import { useApi } from '../../shared/useApi'

// Memoria final, 5.2: "muchos de los usuarios son obligados judicialmente a acceder al plan (...) La información
// que proporcionan las instituciones que administran estos perfiles". La ve todo el personal; la administración
// la completa
export function ReferralPanel({ patientId, canManage }: { patientId: string; canManage: boolean }) {
  const { t, i18n } = useTranslation()
  const { data, error, loading, reload } = useApi(() => getReferral(patientId), [patientId])
  const [referral, setReferral] = useState<Referral | null>(null)
  const [editing, setEditing] = useState(false)
  useEffect(() => setReferral(data), [data])

  return (
    <section className="panel" aria-labelledby="referral-title">
      <div className="panel__header">
        <h2 id="referral-title">{t('referral.title')}</h2>
        {canManage && referral && !editing && (
          <button type="button" className="text-action" onClick={() => setEditing(true)}>
            {t('common.edit')}
          </button>
        )}
      </div>
      <ErrorNotice error={error} onRetry={reload} />
      {loading && !referral ? (
        <LoadingRows rows={2} label={t('common.loading')} />
      ) : referral && editing ? (
        <ReferralForm
          initial={referral}
          onSaved={(saved) => {
            setReferral(saved)
            setEditing(false)
          }}
          onCancel={() => setEditing(false)}
          patientId={patientId}
        />
      ) : (
        referral && (
          <dl className="kv-list kv-list--four">
            <Field label={t('referral.institution')}>{referral.referringInstitution}</Field>
            <Field label={t('referral.contactName')}>{referral.contactName}</Field>
            <Field label={t('referral.contactPhone')}>
              {referral.contactPhone && <a href={`tel:${referral.contactPhone}`}>{referral.contactPhone}</a>}
            </Field>
            <Field label={t('referral.courtOrdered')}>{referral.courtOrdered ? t('referral.yes') : t('referral.no')}</Field>
            {referral.courtOrdered && (
              <>
                <Field label={t('referral.court')} span="two">{referral.court}</Field>
                <Field label={t('referral.measureEndDate')} numeric>
                  {referral.measureEndDate ? formatDate(referral.measureEndDate, i18n.language) : null}
                </Field>
              </>
            )}
            <Field label={t('referral.notes')} span="all">
              {referral.notes && <span className="pre-line">{referral.notes}</span>}
            </Field>
          </dl>
        )
      )}
    </section>
  )
}

function ReferralForm({ patientId, initial, onSaved, onCancel }: {
  patientId: string
  initial: Referral
  onSaved: (referral: Referral) => void
  onCancel: () => void
}) {
  const { t } = useTranslation()
  const [form, setForm] = useState<ReferralInput>({
    referringInstitution: initial.referringInstitution,
    contactName: initial.contactName,
    contactPhone: initial.contactPhone,
    courtOrdered: initial.courtOrdered,
    court: initial.court,
    measureEndDate: initial.measureEndDate,
    notes: initial.notes,
  })
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  const text = (key: keyof ReferralInput) => (value: string) => setForm({ ...form, [key]: value === '' ? null : value })

  async function save(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      onSaved(await updateReferral(patientId, form))
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="form-grid" onSubmit={save} noValidate>
      <div className="form-grid--full">
        <ErrorNotice error={error} title={t('form.notSaved')} />
      </div>
      <div className="form-grid--full">
        <TextInput id="referral-institution" labelText={t('referral.institution')} helperText={t('referral.institutionHelp')}
          value={form.referringInstitution ?? ''} maxLength={150} onChange={(e) => text('referringInstitution')(e.target.value)} />
      </div>
      <TextInput id="referral-contact" labelText={t('referral.contactName')} value={form.contactName ?? ''} maxLength={100}
        onChange={(e) => text('contactName')(e.target.value)} />
      <TextInput id="referral-phone" type="tel" labelText={t('referral.contactPhone')} value={form.contactPhone ?? ''}
        maxLength={20} onChange={(e) => text('contactPhone')(e.target.value)} />
      <div className="form-grid--full">
        <Checkbox id="referral-court-ordered" labelText={t('referral.courtOrderedLabel')} checked={form.courtOrdered}
          onChange={(_, { checked }) => setForm({ ...form, courtOrdered: checked })} />
      </div>
      {form.courtOrdered && (
        <>
          <TextInput id="referral-court" labelText={t('referral.court')} helperText={t('referral.courtHelp')}
            value={form.court ?? ''} maxLength={150} onChange={(e) => text('court')(e.target.value)} />
          <TextInput id="referral-end" type="date" labelText={t('referral.measureEndDate')}
            value={form.measureEndDate ?? ''} onChange={(e) => text('measureEndDate')(e.target.value)} />
        </>
      )}
      <div className="form-grid--full">
        <TextArea id="referral-notes" labelText={t('referral.notes')} helperText={t('referral.notesHelp')}
          value={form.notes ?? ''} rows={4} maxCount={REFERRAL_NOTES_MAX_LENGTH} enableCounter
          onChange={(e) => text('notes')(e.target.value)} />
      </div>
      <div className="form-actions form-grid--full">
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
