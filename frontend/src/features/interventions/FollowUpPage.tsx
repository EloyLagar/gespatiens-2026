import { Button, Select, SelectItem, TextArea, TextInput } from '@carbon/react'
import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import {
  assignTutors,
  deleteIntervention,
  editIntervention,
  getFollowUp,
  INTERVENTION_MAX_LENGTH,
  recordIntervention,
  type FollowUp,
  type FollowUpStaff,
  type Intervention,
} from '../../api/interventions'
import { listUsers } from '../../api/users'
import { useAuth } from '../../auth/AuthContext'
import { formatDate, todayIso } from '../../shared/dates'
import { ConfirmDialog, ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { initialOf, tintFor } from '../users/people'

// El seguimiento de un residente: sus tutores y sus intervenciones. La memoria: "para cada intervención se
// indicarán los tutores del usuario y la información de la intervención realizada", y solo sus tutores las crean
export function FollowUpPage() {
  const { patientId = '' } = useParams()
  const { t } = useTranslation()
  const { isAdmin } = useAuth()
  // Los tutores los asigna solo la administración (decisión del centro)
  const canAssignTutors = isAdmin
  const { data, error, loading, reload } = useApi(() => getFollowUp(patientId), [patientId])
  const [followUp, setFollowUp] = useState<FollowUp | null>(null)
  const [deleting, setDeleting] = useState<Intervention | null>(null)
  const [deleteError, setDeleteError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  useEffect(() => setFollowUp(data), [data])

  const breadcrumb = <Link to="/intervenciones">{t('nav.interventions')}</Link>
  if (error && !followUp) {
    return (
      <>
        <PageHeader title={t('interventions.notFoundTitle')} breadcrumb={breadcrumb} />
        <ErrorNotice error={error} onRetry={reload} />
      </>
    )
  }
  if (loading || !followUp) {
    return <LoadingRows rows={6} label={t('common.loading')} />
  }

  const { resident } = followUp
  const fullName = `${resident.name} ${resident.surname}`

  function replace(saved: Intervention) {
    if (!followUp) return
    const others = followUp.interventions.filter((i) => i.id !== saved.id)
    // De la más reciente a la más antigua, como las devuelve la API
    const sorted = [...others, saved].sort((a, b) => b.date.localeCompare(a.date) || b.createdAt.localeCompare(a.createdAt))
    setFollowUp({ ...followUp, interventions: sorted })
  }

  return (
    <>
      <PageHeader
        title={fullName}
        breadcrumb={breadcrumb}
        subtitle={resident.active ? t('interventions.place', { number: resident.number }) : t('interventions.former')}
        actions={
          <Link className="cds--btn cds--btn--tertiary" to={`/residentes/${resident.id}`}>
            {t('interventions.openRecord')}
          </Link>
        }
      />

      <TutorsPanel
        followUp={followUp}
        canAssign={canAssignTutors && resident.active}
        // Cambiar tutores cambia quién puede registrar: se recarga para que canRecord sea el de la API
        onSaved={(tutors) => { setFollowUp({ ...followUp, tutors }); reload() }}
      />

      <section className="list-section" aria-labelledby="interventions-title">
        <div className="toolbar">
          <h2 id="interventions-title" className="toolbar__title">
            {t('interventions.historyTitle')}
          </h2>
          <p className="toolbar__count num">{t('interventions.historyCount', { count: followUp.interventions.length })}</p>
        </div>

        {followUp.canRecord ? (
          <InterventionForm
            idPrefix="intervention-new"
            submitLabel={t('interventions.record')}
            onSubmit={async (date, text) => replace(await recordIntervention(resident.id, date, text))}
            resetAfterSave
          />
        ) : (
          resident.active && <p className="section-help">{t('interventions.onlyTutors')}</p>
        )}

        {followUp.interventions.length === 0 ? (
          <p className="empty">{t('interventions.historyEmpty')}</p>
        ) : (
          <ol className="notices">
            {followUp.interventions.map((intervention) => (
              <InterventionItem
                key={intervention.id}
                intervention={intervention}
                onSaved={replace}
                onDelete={() => setDeleting(intervention)}
              />
            ))}
          </ol>
        )}
      </section>

      <ConfirmDialog
        open={deleting !== null}
        danger
        title={t('interventions.deleteTitle')}
        confirmLabel={t('interventions.delete')}
        busy={busy}
        error={deleteError}
        onClose={() => { setDeleting(null); setDeleteError(null) }}
        onConfirm={async () => {
          if (!deleting) return
          setBusy(true)
          setDeleteError(null)
          try {
            await deleteIntervention(deleting.id)
            setFollowUp({ ...followUp, interventions: followUp.interventions.filter((i) => i.id !== deleting.id) })
            setDeleting(null)
          } catch (err) {
            setDeleteError(err)
          } finally {
            setBusy(false)
          }
        }}
      >
        {deleting && (
          <>
            <p>{t('interventions.deleteBody', { name: fullName })}</p>
            <blockquote className="notice-quote">{deleting.text}</blockquote>
          </>
        )}
      </ConfirmDialog>
    </>
  )
}

function TutorsPanel({ followUp, canAssign, onSaved }: {
  followUp: FollowUp
  canAssign: boolean
  onSaved: (tutors: FollowUpStaff[]) => void
}) {
  const { t } = useTranslation()
  const [editing, setEditing] = useState(false)

  return (
    <section className="panel" aria-labelledby="tutors-title">
      <div className="panel__header">
        <h2 id="tutors-title">{t('interventions.tutors')}</h2>
        {canAssign && !editing && (
          <button type="button" className="text-action" onClick={() => setEditing(true)}>
            {t('interventions.changeTutors')}
          </button>
        )}
      </div>
      {editing ? (
        <TutorsForm
          patientId={followUp.resident.id}
          current={followUp.tutors}
          onSaved={(tutors) => { onSaved(tutors); setEditing(false) }}
          onCancel={() => setEditing(false)}
        />
      ) : followUp.tutors.length === 0 ? (
        <p className="empty">{canAssign ? t('interventions.noTutorsAdmin') : t('interventions.noTutorsYet')}</p>
      ) : (
        <ul className="tutors">
          {followUp.tutors.map((tutor) => (
            <li key={tutor.id} className="tutors__item">
              <span className="avatar" style={{ background: tintFor(tutor.id) }} aria-hidden="true">
                {initialOf(tutor.name)}
              </span>
              <span className="notice__who">
                <span className="notice__author">{tutor.name}</span>
                <span className="notice__meta">{t(`speciality.${tutor.speciality}`)}</span>
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function TutorsForm({ patientId, current, onSaved, onCancel }: {
  patientId: string
  current: FollowUpStaff[]
  onSaved: (tutors: FollowUpStaff[]) => void
  onCancel: () => void
}) {
  const { t } = useTranslation()
  const staff = useApi(listUsers, [])
  // Memoria final: un tutor educador social y un tutor psicólogo
  const [educator, setEducator] = useState(() => current.find((t) => t.speciality === 'SOCIAL_EDUCATOR')?.id ?? '')
  const [psychologist, setPsychologist] = useState(() => current.find((t) => t.speciality === 'PSYCHOLOGIST')?.id ?? '')
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  const of = (speciality: string) =>
    (staff.data ?? []).filter((u) => u.speciality === speciality).sort((a, b) => a.name.localeCompare(b.name))

  async function save(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      onSaved(await assignTutors(patientId, educator || null, psychologist || null))
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={save} className="shift__form" noValidate>
      <ErrorNotice error={error ?? staff.error} title={t('form.notSaved')} />
      {staff.loading && !staff.data ? (
        <LoadingRows rows={3} label={t('common.loading')} />
      ) : (
        <div className="tutor-picker">
          <Select id="tutor-educator" labelText={t('interventions.educatorTutor')} value={educator}
            onChange={(e) => setEducator(e.target.value)}>
            <SelectItem value="" text={t('interventions.unassigned')} />
            {of('SOCIAL_EDUCATOR').map((u) => <SelectItem key={u.id} value={u.id} text={u.name} />)}
          </Select>
          <Select id="tutor-psychologist" labelText={t('interventions.psychologistTutor')} value={psychologist}
            onChange={(e) => setPsychologist(e.target.value)}>
            <SelectItem value="" text={t('interventions.unassigned')} />
            {of('PSYCHOLOGIST').map((u) => <SelectItem key={u.id} value={u.id} text={u.name} />)}
          </Select>
        </div>
      )}
      <div className="form-actions">
        <Button kind="secondary" size="sm" type="button" onClick={onCancel} disabled={busy}>
          {t('common.cancel')}
        </Button>
        <Button size="sm" type="submit" disabled={busy || !staff.data}>
          {busy ? t('common.saving') : t('common.save')}
        </Button>
      </div>
    </form>
  )
}

interface FormProps {
  idPrefix: string
  initialDate?: string
  initialText?: string
  submitLabel: string
  onSubmit: (date: string, text: string) => Promise<void>
  onCancel?: () => void
  resetAfterSave?: boolean
}

// Sirve para registrar una intervención nueva y para corregir una
function InterventionForm({ idPrefix, initialDate, initialText = '', submitLabel, onSubmit, onCancel, resetAfterSave }: FormProps) {
  const { t } = useTranslation()
  const today = todayIso()
  const [date, setDate] = useState(initialDate ?? today)
  const [text, setText] = useState(initialText)
  const [invalid, setInvalid] = useState<string | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!date || date > today) return setInvalid(t('interventions.dateInvalid'))
    if (!text.trim()) return setInvalid(t('interventions.textNeeded'))
    setInvalid(null)
    setBusy(true)
    setError(null)
    try {
      await onSubmit(date, text)
      if (resetAfterSave) {
        setText('')
        setDate(todayIso())
      }
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="panel composer intervention-form" onSubmit={save} noValidate>
      <ErrorNotice error={error} title={t('form.notSaved')} />
      <TextInput
        id={`${idPrefix}-date`}
        type="date"
        labelText={t('interventions.date')}
        max={today}
        value={date}
        onChange={(e) => setDate(e.target.value)}
      />
      <TextArea
        id={`${idPrefix}-text`}
        labelText={t('interventions.text')}
        helperText={t('interventions.textHelp')}
        value={text}
        onChange={(e) => setText(e.target.value)}
        rows={3}
        maxCount={INTERVENTION_MAX_LENGTH}
        enableCounter
      />
      {invalid && <p className="form-error" role="alert">{invalid}</p>}
      <div className="form-actions">
        {onCancel && (
          <Button kind="secondary" size="sm" type="button" onClick={onCancel} disabled={busy}>
            {t('common.cancel')}
          </Button>
        )}
        <Button size="sm" type="submit" disabled={busy}>
          {busy ? t('common.saving') : submitLabel}
        </Button>
      </div>
    </form>
  )
}

function InterventionItem({ intervention, onSaved, onDelete }: {
  intervention: Intervention
  onSaved: (i: Intervention) => void
  onDelete: () => void
}) {
  const { t, i18n } = useTranslation()
  const [editing, setEditing] = useState(false)
  const author = intervention.author
  const titleId = `intervention-${intervention.id}-title`

  return (
    <li>
      <article className={intervention.mine ? 'notice notice--mine' : 'notice'} aria-labelledby={titleId}>
        <header className="notice__header">
          <span className="avatar" style={{ background: author ? tintFor(author.id) : undefined }} aria-hidden="true">
            {author ? initialOf(author.name) : '·'}
          </span>
          <span className="notice__who">
            <span id={titleId} className="notice__author">
              <time dateTime={intervention.date}>{formatDate(intervention.date, i18n.language)}</time>
              {' · '}
              {author ? author.name : t('notices.formerEmployee')}
              {intervention.mine && <span className="you"> ({t('employees.you')})</span>}
            </span>
            <span className="notice__meta">
              {author && t(`speciality.${author.speciality}`)}
              {intervention.editedAt && <>{author && ' · '}{t('notices.edited')}</>}
            </span>
          </span>
        </header>

        {editing ? (
          <InterventionForm
            idPrefix={`intervention-${intervention.id}`}
            initialDate={intervention.date}
            initialText={intervention.text}
            submitLabel={t('common.save')}
            onSubmit={async (date, text) => {
              onSaved(await editIntervention(intervention.id, date, text))
              setEditing(false)
            }}
            onCancel={() => setEditing(false)}
          />
        ) : (
          <p className="notice__text">{intervention.text}</p>
        )}

        {intervention.manageable && !editing && (
          <footer className="notice__actions">
            <button type="button" className="text-action" onClick={() => setEditing(true)}>
              {t('common.edit')}
            </button>
            <button type="button" className="text-action text-action--danger" onClick={onDelete}>
              {t('interventions.delete')}
            </button>
          </footer>
        )}
      </article>
    </li>
  )
}
