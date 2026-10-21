import { Button, InlineNotification, TextArea, TextInput } from '@carbon/react'
import { Download, Edit, View } from '@carbon/icons-react'
import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { beingEditedBy, listLocks, reportResources, type EditLock } from '../../api/editLocks'
import {
  closeReport,
  editReportHeader,
  getReport,
  reopenReport,
  reportPdf,
  SECTION_MAX_LENGTH,
  sectionConflictOf,
  writeSection,
  type Report,
  type ReportAreaInfo,
  type ReportHeaderInput,
  type ReportSection,
  type SectionConflict,
} from '../../api/reports'
import { formatDate } from '../../shared/dates'
import { previewFile, saveFile } from '../../shared/files'
import { whenPosted } from '../../shared/times'
import { ConfirmDialog, ErrorNotice, Field, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { useEditLock, type LockState } from '../../shared/useEditLock'
import { initialOf, tintFor } from '../users/people'
import { ReportStateTag } from './ReportsPage'

type Dialog = 'close' | 'reopen' | null

// Cada cuánto se mira quién está editando qué en el informe
const LOCKS_POLL_MS = 15_000

// Un informe de media estancia o de fin de programa, con las áreas y apartados del modelo del centro.
// RF8: cada apartado se edita por separado y mientras alguien lo edita nadie más puede; los demás ven quién es
export function ReportPage() {
  const { id = '' } = useParams()
  const { t, i18n } = useTranslation()
  const { data, error, loading, reload } = useApi(() => getReport(id), [id])
  const [report, setReport] = useState<Report | null>(null)
  const [others, setOthers] = useState<Map<string, EditLock>>(new Map())
  const [dirty, setDirty] = useState<Set<string>>(new Set())
  const [dialog, setDialog] = useState<Dialog>(null)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<unknown>(null)
  const [pdfBusy, setPdfBusy] = useState(false)
  const [pdfError, setPdfError] = useState<unknown>(null)
  useEffect(() => setReport(data), [data])

  // Quién está editando qué. Cuando alguien termina, se recarga el informe para ver lo que ha guardado
  useEffect(() => {
    let cancelled = false
    let previous = ''
    const poll = () =>
      listLocks(reportResources.prefix(id))
        .then((locks) => {
          if (cancelled) return
          const theirs = locks.filter((lock) => !lock.mine)
          const key = theirs.map((lock) => lock.resource).join('|')
          if (previous && key !== previous) reload()
          previous = key
          setOthers(new Map(theirs.map((lock) => [lock.resource, lock])))
        })
        .catch(() => {})
    poll()
    const timer = window.setInterval(poll, LOCKS_POLL_MS)
    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [id, reload])

  const markDirty = useCallback((key: string, isDirty: boolean) => {
    setDirty((current) => {
      if (current.has(key) === isDirty) return current
      const next = new Set(current)
      if (isDirty) next.add(key)
      else next.delete(key)
      return next
    })
  }, [])

  // Avisa antes de cerrar la pestaña con apartados sin guardar
  useEffect(() => {
    if (dirty.size === 0) return
    const warn = (event: BeforeUnloadEvent) => event.preventDefault()
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [dirty.size])

  const breadcrumb = <Link to="/informes">{t('nav.reports')}</Link>
  if (error && !report) {
    return (
      <>
        <PageHeader title={t('reports.notFoundTitle')} breadcrumb={breadcrumb} />
        <ErrorNotice error={error} onRetry={reload} />
      </>
    )
  }
  if (loading || !report) {
    return <LoadingRows rows={8} label={t('common.loading')} />
  }

  const { resident } = report
  const fullName = `${resident.name} ${resident.surname}`
  const draft = report.state === 'DRAFT'

  function closeDialog() {
    setDialog(null)
    setActionError(null)
  }

  async function run(action: () => Promise<void>) {
    setBusy(true)
    setActionError(null)
    try {
      await action()
      closeDialog()
    } catch (err) {
      setActionError(err)
    } finally {
      setBusy(false)
    }
  }

  async function pdf(preview: boolean) {
    setPdfBusy(true)
    setPdfError(null)
    try {
      if (preview) await previewFile(() => reportPdf(id, true))
      else saveFile(await reportPdf(id))
    } catch (err) {
      setPdfError(err)
    } finally {
      setPdfBusy(false)
    }
  }

  return (
    <>
      <PageHeader
        title={t(`reports.type.${report.type}`)}
        breadcrumb={breadcrumb}
        subtitle={
          <>
            <Link to={`/residentes/${resident.id}`}>{fullName}</Link>
            {' · '}
            {t('reports.stay', {
              from: formatDate(report.stayStart, i18n.language),
              to: report.header.exitDate ? formatDate(report.header.exitDate, i18n.language) : t('reports.stayOngoing'),
            })}
            {' · '}
            <ReportStateTag state={report.state} />
          </>
        }
        actions={
          <>
            <Button kind="ghost" renderIcon={View} onClick={() => pdf(true)} disabled={pdfBusy}>
              {t('reports.preview')}
            </Button>
            <Button kind="tertiary" renderIcon={Download} onClick={() => pdf(false)} disabled={pdfBusy}>
              {pdfBusy ? t('reports.downloading') : t('reports.downloadPdf')}
            </Button>
            {report.manageable && draft && (
              <Button onClick={() => setDialog('close')} disabled={dirty.size > 0 || others.size > 0}>
                {t('reports.close')}
              </Button>
            )}
            {report.manageable && !draft && (
              <Button kind="secondary" onClick={() => setDialog('reopen')}>
                {t('reports.reopen')}
              </Button>
            )}
          </>
        }
      />
      <ErrorNotice error={pdfError} title={t('reports.pdfFailed')} />

      {draft ? (
        <p className="section-help">
          {t('reports.progress', { written: report.writtenSections, total: report.totalSections })}
          {others.size > 0 && <> · {t('reports.othersEditing', { count: others.size })}</>}
          {dirty.size > 0 && <> · <strong>{t('reports.unsaved', { count: dirty.size })}</strong></>}
        </p>
      ) : (
        <p className="notice">
          {t('reports.closedNotice', {
            date: report.closedAt ? whenPosted(report.closedAt, t, i18n.language) : '',
            name: report.closedBy?.name ?? t('notices.formerEmployee'),
          })}
        </p>
      )}

      <HeaderPanel
        report={report}
        heldBy={others.get(reportResources.header(report.id))}
        onSaved={setReport}
        onDirty={(d) => markDirty('header', d)}
      />

      {report.areas.map((area) => (
        <AreaPanel
          key={area.area}
          report={report}
          area={area}
          others={others}
          onSaved={setReport}
          onDirty={markDirty}
          onStale={reload}
        />
      ))}

      <section className="panel" aria-labelledby="authors-title">
        <h2 id="authors-title">{t('reports.authorsTitle')}</h2>
        {report.authors.length === 0 ? (
          <p className="empty">{t('reports.authorsEmpty')}</p>
        ) : (
          <ul className="tutors">
            {report.authors.map((author) => (
              <li key={author.id} className="tutors__item">
                <span className="avatar" style={{ background: tintFor(author.id) }} aria-hidden="true">
                  {initialOf(author.name)}
                </span>
                <span className="notice__who">
                  <span className="notice__author">{author.name}</span>
                  <span className="notice__meta">{t(`speciality.${author.speciality}`)}</span>
                </span>
              </li>
            ))}
          </ul>
        )}
        <p className="section-help report-header__note">{t('reports.signaturesHelp')}</p>
      </section>

      <ConfirmDialog
        open={dialog === 'close'}
        title={t('reports.closeTitle', { name: fullName })}
        confirmLabel={t('reports.close')}
        busy={busy}
        error={actionError}
        onClose={closeDialog}
        onConfirm={() => run(async () => setReport(await closeReport(report.id)))}
      >
        <p>{t('reports.closeBody', { written: report.writtenSections, total: report.totalSections })}</p>
      </ConfirmDialog>

      <ConfirmDialog
        open={dialog === 'reopen'}
        title={t('reports.reopenTitle', { name: fullName })}
        confirmLabel={t('reports.reopen')}
        busy={busy}
        error={actionError}
        onClose={closeDialog}
        onConfirm={() => run(async () => setReport(await reopenReport(report.id)))}
      >
        <p>{t('reports.reopenBody')}</p>
      </ConfirmDialog>
    </>
  )
}

// "Marta lo está editando": lo ve quien no lo tiene abierto
function EditingBadge({ name }: { name: string | null }) {
  const { t } = useTranslation()
  return (
    <span className="editing-badge">
      <Edit size={16} aria-hidden="true" />
      {t('reports.lock.editing', { name: name ?? t('notices.formerEmployee') })}
    </span>
  )
}

// El aviso cuando no se ha podido empezar a editar o se ha perdido el bloqueo
function LockNotice({ state }: { state: LockState }) {
  const { t } = useTranslation()
  if (state.status === 'busy') {
    return (
      <p className="form-error" role="alert">
        {t('reports.lock.busy', { name: state.holderName ?? t('notices.formerEmployee') })}
      </p>
    )
  }
  if (state.status === 'error') return <ErrorNotice error={state.error} />
  return null
}

// La cabecera: los datos personales y las fechas salen de la ficha; el resto lo completa cualquier empleado
function HeaderPanel({ report, heldBy, onSaved, onDirty }: {
  report: Report
  heldBy: EditLock | undefined
  onSaved: (report: Report) => void
  onDirty: (dirty: boolean) => void
}) {
  const { t, i18n } = useTranslation()
  const lock = useEditLock(reportResources.header(report.id))
  const [editing, setEditing] = useState(false)
  const { resident, header } = report
  const midStay = report.type === 'MID_STAY'
  const date = (iso: string | null) => (iso ? formatDate(iso, i18n.language) : null)

  return (
    <section className="panel" aria-labelledby="report-header-title">
      <div className="panel__header">
        <h2 id="report-header-title">{t('reports.header.title')}</h2>
        {heldBy && !editing && <EditingBadge name={heldBy.holderName} />}
        {report.state === 'DRAFT' && !editing && !heldBy && (
          <button
            type="button"
            className="text-action"
            onClick={async () => setEditing(await lock.acquire())}
            disabled={lock.state.status === 'acquiring'}
          >
            {t('reports.header.edit')}
          </button>
        )}
      </div>
      <LockNotice state={lock.state} />
      {editing ? (
        <HeaderForm
          report={report}
          onSaved={(saved) => {
            onSaved(saved)
            onDirty(false)
            setEditing(false)
            lock.stop()
          }}
          onCancel={() => {
            onDirty(false)
            setEditing(false)
            lock.stop()
          }}
          onDirty={onDirty}
        />
      ) : (
        <dl className="kv-list kv-list--four">
          <Field label={t('reports.header.fullName')}>{`${resident.surname}, ${resident.name}`}</Field>
          <Field label={t('patient.birthDate')} numeric>{date(resident.birthDate)}</Field>
          <Field label={t('patient.dni')}>{resident.dni}</Field>
          {midStay ? (
            <Field label={t('reports.header.nip')}>{header.nip}</Field>
          ) : (
            <Field label={t('patient.sip')}>{resident.sip}</Field>
          )}
          <Field label={t('reports.header.referenceCenter')}>{header.referenceCenter}</Field>
          <Field label={t('reports.header.requestNumber')}>{header.requestNumber}</Field>
          <Field label={midStay ? t('reports.header.entryDateMidStay') : t('reports.header.entryDate')} numeric>
            {date(header.entryDate)}
          </Field>
          {!midStay && <Field label={t('reports.header.exitDate')} numeric>{date(header.exitDate)}</Field>}
          {!midStay && (
            <div className="kv-span kv-span--all">
              <Field label={t('reports.header.exitReason')}>{header.exitReason}</Field>
            </div>
          )}
        </dl>
      )}
      <p className="section-help report-header__note">{t('reports.header.fromRecord')}</p>
    </section>
  )
}

function HeaderForm({ report, onSaved, onCancel, onDirty }: {
  report: Report
  onSaved: (report: Report) => void
  onCancel: () => void
  onDirty: (dirty: boolean) => void
}) {
  const { t } = useTranslation()
  const { header } = report
  const midStay = report.type === 'MID_STAY'
  const [form, setForm] = useState<ReportHeaderInput>({
    requestNumber: header.requestNumber,
    referenceCenter: header.referenceCenter,
    nip: header.nip,
    exitReason: header.exitReason,
  })
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  function set(key: keyof ReportHeaderInput, value: string) {
    setForm({ ...form, [key]: value === '' ? null : value })
    onDirty(true)
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      onSaved(await editReportHeader(report.id, form, header.version))
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={save} className="form-grid" noValidate>
      <div className="form-grid--full">
        <ErrorNotice error={error} title={t('form.notSaved')} />
      </div>
      <TextInput
        id="header-reference-center"
        labelText={t('reports.header.referenceCenter')}
        value={form.referenceCenter ?? ''}
        maxLength={150}
        onChange={(e) => set('referenceCenter', e.target.value)}
      />
      <TextInput
        id="header-request-number"
        labelText={t('reports.header.requestNumber')}
        value={form.requestNumber ?? ''}
        maxLength={30}
        onChange={(e) => set('requestNumber', e.target.value)}
      />
      {midStay ? (
        <TextInput
          id="header-nip"
          labelText={t('reports.header.nip')}
          value={form.nip ?? ''}
          maxLength={30}
          onChange={(e) => set('nip', e.target.value)}
        />
      ) : (
        <TextInput
          id="header-exit-reason"
          labelText={t('reports.header.exitReason')}
          value={form.exitReason ?? ''}
          maxLength={200}
          onChange={(e) => set('exitReason', e.target.value)}
        />
      )}
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

// Un área del informe con sus apartados. Quien no la redacta la lee
function AreaPanel({ report, area, others, onSaved, onDirty, onStale }: {
  report: Report
  area: ReportAreaInfo
  others: Map<string, EditLock>
  onSaved: (report: Report) => void
  onDirty: (key: string, dirty: boolean) => void
  onStale: () => void
}) {
  const { t } = useTranslation()
  const sections = report.sections.filter((s) => s.area === area.area)
  const editable = report.state === 'DRAFT' && area.writable
  const titleId = `area-${area.area}`
  const firstAdaptation = sections.findIndex((s) => s.adaptation)
  const writers = [
    ...area.writers.map((w) => t(`speciality.${w}`)),
    ...(area.tutors ? [t('reports.tutorsOfResident')] : []),
    t('speciality.ADMINISTRATOR'),
  ].join(', ')

  return (
    <section className="panel report-area" aria-labelledby={titleId}>
      <div className="panel__header">
        <h2 id={titleId}>{t(`reports.area.${area.area}`)}</h2>
        <p className="panel__meta">{area.writable ? t('reports.youWrite') : t('reports.writtenBy', { who: writers })}</p>
      </div>
      <ol className="report-sections">
        {sections.map((section, index) => (
          <li key={section.section} className={section.adaptation ? 'report-section report-section--nested' : 'report-section'}>
            {index === firstAdaptation && <p className="report-sections__group">{t('reports.adaptationHeading')}</p>}
            <SectionEditor
              reportId={report.id}
              section={section}
              editable={editable}
              heldBy={others.get(reportResources.section(report.id, section.section))}
              onSaved={onSaved}
              onDirty={(dirty) => onDirty(section.section, dirty)}
              onStale={onStale}
            />
          </li>
        ))}
      </ol>
    </section>
  )
}

// Un apartado. Para escribir hay que pulsar "Editar": así se pide el bloqueo (RF8) y mientras tanto nadie más
// puede editarlo. La versión de partida se guarda por si, aun así, el apartado cambió (p. ej. el bloqueo
// caducó porque el equipo se quedó sin red): entonces se enseñan las dos versiones para elegir
export function SectionEditor({ reportId, section, editable, heldBy, onSaved, onDirty, onStale }: {
  reportId: string
  section: ReportSection
  editable: boolean
  heldBy: EditLock | undefined
  onSaved: (report: Report) => void
  onDirty: (dirty: boolean) => void
  onStale: () => void
}) {
  const { t, i18n } = useTranslation()
  const lock = useEditLock(reportResources.section(reportId, section.section))
  // Abierto para editar. Si se pierde el bloqueo a medias, sigue abierto para no perder lo escrito
  const [editing, setEditing] = useState(false)
  const [text, setText] = useState(section.text ?? '')
  const [base, setBase] = useState(section.version)
  const [conflict, setConflict] = useState<SectionConflict | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [justSaved, setJustSaved] = useState(false)
  const dirty = editing && text.trim() !== (section.text ?? '').trim()
  const label = t(`reports.sections.${section.section}`)

  useEffect(() => onDirty(dirty), [dirty, onDirty])

  async function startEditing() {
    setJustSaved(false)
    setError(null)
    if (await lock.acquire()) {
      // Se parte de lo que hay guardado ahora
      setText(section.text ?? '')
      setBase(section.version)
      setConflict(null)
      setEditing(true)
    }
  }

  function stopEditing() {
    setConflict(null)
    setError(null)
    setEditing(false)
    lock.stop()
  }

  async function save(version: number) {
    setBusy(true)
    setError(null)
    try {
      const report = await writeSection(reportId, section.section, text, version)
      stopEditing()
      setJustSaved(true)
      onSaved(report)
    } catch (err) {
      const found = sectionConflictOf(err)
      if (found) setConflict(found)
      else setError(err)
    } finally {
      setBusy(false)
    }
  }

  function keepTheirs() {
    stopEditing()
    // Para ver ya lo que guardó la otra persona
    onStale()
  }

  const meta = section.updatedAt && (
    <span className="report-section__meta">
      {section.author?.name ?? t('notices.formerEmployee')} ·{' '}
      <time dateTime={section.updatedAt}>{whenPosted(section.updatedAt, t, i18n.language)}</time>
    </span>
  )

  if (!editing) {
    const lockedByOther = heldBy !== undefined
    return (
      <div className="report-section__read">
        <div className="report-section__head">
          <h3 className="report-section__label">{label}</h3>
          {lockedByOther && <EditingBadge name={heldBy.holderName} />}
          {editable && !lockedByOther && (
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Edit}
              onClick={startEditing}
              disabled={lock.state.status === 'acquiring'}
              aria-label={t('reports.editSection', { section: label })}
            >
              {t('common.edit')}
            </Button>
          )}
        </div>
        {section.text ? <p className="report-section__text">{section.text}</p> : <p className="muted">{t('reports.empty.section')}</p>}
        {meta}
        <LockNotice state={lock.state} />
        <span role="status" aria-live="polite" className="report-section__status">
          {justSaved && t('reports.savedSection')}
        </span>
      </div>
    )
  }

  const lockLost = beingEditedBy(error) ?? (lock.state.status === 'busy' ? { name: lock.state.holderName } : null)
  return (
    <div className="report-section__edit">
      <TextArea
        id={`section-${section.section}`}
        labelText={label}
        value={text}
        rows={text.length > 300 ? 8 : 4}
        maxCount={SECTION_MAX_LENGTH}
        enableCounter
        onChange={(e) => setText(e.target.value)}
      />
      {conflict && (
        <div className="report-conflict" role="alert">
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={t('reports.conflict.title')}
            subtitle={t('reports.conflict.help')}
          />
          <p className="report-conflict__label">{t('reports.conflict.theirs')}</p>
          {conflict.currentText ? (
            <blockquote className="notice-quote">{conflict.currentText}</blockquote>
          ) : (
            <p className="muted">{t('reports.conflict.emptied')}</p>
          )}
          <div className="form-actions">
            <Button kind="secondary" size="sm" type="button" onClick={keepTheirs} disabled={busy}>
              {t('reports.conflict.keepTheirs')}
            </Button>
            <Button kind="danger--tertiary" size="sm" type="button" onClick={() => save(conflict.currentVersion)} disabled={busy}>
              {t('reports.conflict.overwrite')}
            </Button>
          </div>
        </div>
      )}
      {lockLost ? (
        <p className="form-error" role="alert">
          {t('reports.lock.lost', { name: lockLost.name ?? t('notices.formerEmployee') })}
        </p>
      ) : (
        <ErrorNotice error={error} title={t('form.notSaved')} />
      )}
      {!conflict && (
        <div className="report-section__footer">
          {meta}
          <span className="report-section__actions">
            <Button kind="ghost" size="sm" type="button" disabled={busy} onClick={stopEditing}>
              {t('common.cancel')}
            </Button>
            <Button size="sm" type="button" disabled={busy || !dirty} onClick={() => save(base)}>
              {busy ? t('common.saving') : t('reports.saveSection')}
            </Button>
          </span>
        </div>
      )}
    </div>
  )
}
