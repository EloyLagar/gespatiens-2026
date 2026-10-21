import { Button, TextInput } from '@carbon/react'
import { Add } from '@carbon/icons-react'
import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import {
  authorizeVisitor,
  listVisitors,
  revokeVisitor,
  updateVisitor,
  type Visitor,
  type VisitorInput,
} from '../../api/patients'
import { ConfirmDialog, ErrorNotice, LoadingRows } from '../../shared/ui'
import { useApi } from '../../shared/useApi'

const EMPTY: VisitorInput = { name: '', relationship: '', phoneNumber: '' }

// Memoria final: "tendrán un apartado dentro de su ficha en el que se podrá gestionar las personas que están
// autorizadas a visitarles, la relación de estas con el usuario y su número de teléfono"
export function VisitorsPanel({ patientId, canManage }: { patientId: string; canManage: boolean }) {
  const { t } = useTranslation()
  const { data, error, loading, reload } = useApi(() => listVisitors(patientId), [patientId])
  const [visitors, setVisitors] = useState<Visitor[]>([])
  // null: nada abierto; 'new': añadiendo; un id: corrigiendo esa persona
  const [editing, setEditing] = useState<string | null>(null)
  const [revoking, setRevoking] = useState<Visitor | null>(null)
  const [revokeError, setRevokeError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  useEffect(() => setVisitors(data ?? []), [data])

  const sorted = (list: Visitor[]) => [...list].sort((a, b) => a.name.localeCompare(b.name))

  return (
    <section className="panel" aria-labelledby="visitors-title">
      <div className="panel__header">
        <h2 id="visitors-title">{t('visitors.title')}</h2>
        {canManage && editing === null && (
          <Button kind="ghost" size="sm" renderIcon={Add} onClick={() => setEditing('new')}>
            {t('visitors.add')}
          </Button>
        )}
      </div>
      <ErrorNotice error={error} onRetry={reload} />
      {editing === 'new' && (
        <VisitorForm
          initial={EMPTY}
          submitLabel={t('visitors.authorize')}
          onSubmit={async (input) => {
            const saved = await authorizeVisitor(patientId, input)
            setVisitors(sorted([...visitors, saved]))
            setEditing(null)
          }}
          onCancel={() => setEditing(null)}
        />
      )}
      {loading && !data ? (
        <LoadingRows rows={2} label={t('common.loading')} />
      ) : visitors.length === 0 ? (
        <p className="empty">{t('visitors.none')}</p>
      ) : (
        <ul className="visitors">
          {visitors.map((v) =>
            editing === v.id ? (
              <li key={v.id}>
                <VisitorForm
                  initial={v}
                  submitLabel={t('common.save')}
                  onSubmit={async (input) => {
                    const saved = await updateVisitor(patientId, v.id, input)
                    setVisitors(sorted(visitors.map((x) => (x.id === v.id ? saved : x))))
                    setEditing(null)
                  }}
                  onCancel={() => setEditing(null)}
                />
              </li>
            ) : (
              <li key={v.id} className="visitors__item">
                <span className="visitors__name">{v.name}</span>
                <span className="muted">{v.relationship}</span>
                <a className="num" href={`tel:${v.phoneNumber}`}>{v.phoneNumber}</a>
                {canManage && editing === null && (
                  <span className="row-actions">
                    <button type="button" className="text-action" onClick={() => setEditing(v.id)}
                      aria-label={t('visitors.editWho', { name: v.name })}>
                      {t('common.edit')}
                    </button>
                    <button type="button" className="text-action text-action--danger" onClick={() => setRevoking(v)}
                      aria-label={t('visitors.revokeWho', { name: v.name })}>
                      {t('visitors.revoke')}
                    </button>
                  </span>
                )}
              </li>
            ),
          )}
        </ul>
      )}

      <ConfirmDialog
        open={revoking !== null}
        danger
        title={t('visitors.revokeTitle', { name: revoking?.name ?? '' })}
        confirmLabel={t('visitors.revoke')}
        busy={busy}
        error={revokeError}
        onClose={() => { setRevoking(null); setRevokeError(null) }}
        onConfirm={async () => {
          if (!revoking) return
          setBusy(true)
          setRevokeError(null)
          try {
            await revokeVisitor(patientId, revoking.id)
            setVisitors(visitors.filter((v) => v.id !== revoking.id))
            setRevoking(null)
          } catch (err) {
            setRevokeError(err)
          } finally {
            setBusy(false)
          }
        }}
      >
        <p>{t('visitors.revokeBody')}</p>
      </ConfirmDialog>
    </section>
  )
}

function VisitorForm({ initial, submitLabel, onSubmit, onCancel }: {
  initial: VisitorInput
  submitLabel: string
  onSubmit: (input: VisitorInput) => Promise<void>
  onCancel: () => void
}) {
  const { t } = useTranslation()
  const [form, setForm] = useState<VisitorInput>({
    name: initial.name,
    relationship: initial.relationship,
    phoneNumber: initial.phoneNumber,
  })
  const [missing, setMissing] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!form.name.trim() || !form.relationship.trim() || !form.phoneNumber.trim()) return setMissing(true)
    setMissing(false)
    setBusy(true)
    setError(null)
    try {
      await onSubmit(form)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  const invalid = (value: string) => missing && !value.trim()
  return (
    <form className="visitor-form" onSubmit={save} noValidate>
      <ErrorNotice error={error} title={t('form.notSaved')} />
      <TextInput id="visitor-name" labelText={t('visitors.name')} value={form.name} maxLength={50}
        invalid={invalid(form.name)} invalidText={t('form.required')}
        onChange={(e) => setForm({ ...form, name: e.target.value })} />
      <TextInput id="visitor-relationship" labelText={t('visitors.relationship')} value={form.relationship} maxLength={50}
        placeholder={t('visitors.relationshipHint')} invalid={invalid(form.relationship)} invalidText={t('form.required')}
        onChange={(e) => setForm({ ...form, relationship: e.target.value })} />
      <TextInput id="visitor-phone" type="tel" labelText={t('visitors.phoneNumber')} value={form.phoneNumber} maxLength={20}
        invalid={invalid(form.phoneNumber)} invalidText={t('form.required')}
        onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
      <div className="form-actions">
        <Button kind="secondary" size="sm" type="button" onClick={onCancel} disabled={busy}>
          {t('common.cancel')}
        </Button>
        <Button size="sm" type="submit" disabled={busy}>
          {busy ? t('common.saving') : submitLabel}
        </Button>
      </div>
    </form>
  )
}
