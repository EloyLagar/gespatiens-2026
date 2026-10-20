import { Button, Tag, TextArea } from '@carbon/react'
import { useMemo, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { deleteNotice, editNotice, listNotices, publishNotice } from '../../api/notices'
import { NOTICE_MAX_LENGTH, type Notice } from '../../api/types'
import { ConfirmDialog, ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { whenPosted } from '../../shared/times'
import { useApi } from '../../shared/useApi'
import { initialOf, tintFor } from '../users/people'

type Filter = 'all' | 'mine' | 'administration'

// La pantalla de inicio (RF5). Pregunta: ¿qué tengo que saber hoy?
// La memoria pide diferenciar los avisos propios de los ajenos y destacar los de la administración
export function NoticesPage() {
  const { t } = useTranslation()
  const { data, error, loading, reload } = useApi(listNotices, [])
  const [notices, setNotices] = useState<Notice[] | null>(null)
  const [filter, setFilter] = useState<Filter>('all')
  const [deleting, setDeleting] = useState<Notice | null>(null)
  const [deleteError, setDeleteError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  // La lista local se rellena con la de la API y luego se actualiza al publicar, editar o borrar
  const all = notices ?? data ?? []
  const counts = useMemo(
    () => ({
      all: all.length,
      mine: all.filter((n) => n.mine).length,
      administration: all.filter((n) => n.author?.administrator).length,
    }),
    [all],
  )
  const shown = all.filter((n) =>
    filter === 'mine' ? n.mine : filter === 'administration' ? n.author?.administrator : true,
  )

  function replace(updated: Notice) {
    setNotices(all.map((n) => (n.id === updated.id ? updated : n)))
  }

  return (
    <>
      <PageHeader
        title={t('notices.title')}
        subtitle={data ? t('notices.subtitle', { count: counts.all, admin: counts.administration }) : null}
      />

      <Composer onPublished={(notice) => setNotices([notice, ...all])} />

      <section className="list-section" aria-labelledby="notices-list-title">
        <div className="toolbar">
          <h2 id="notices-list-title" className="toolbar__title">
            {t('notices.listTitle')}
          </h2>
          <div className="segmented" role="group" aria-label={t('notices.filterLabel')}>
            {(['all', 'mine', 'administration'] as Filter[]).map((f) => (
              <button key={f} type="button" aria-pressed={filter === f} onClick={() => setFilter(f)}>
                {t(`notices.filter.${f}`)} <span className="num">{counts[f]}</span>
              </button>
            ))}
          </div>
        </div>

        {error ? (
          <ErrorNotice error={error} onRetry={reload} />
        ) : loading && !data ? (
          <LoadingRows rows={3} label={t('common.loading')} />
        ) : shown.length === 0 ? (
          <p className="empty">{t(`notices.empty.${filter}`)}</p>
        ) : (
          <ol className="notices">
            {shown.map((notice) => (
              <NoticeItem key={notice.id} notice={notice} onSaved={replace} onDelete={() => setDeleting(notice)} />
            ))}
          </ol>
        )}
      </section>

      <ConfirmDialog
        open={deleting !== null}
        danger
        title={t('notices.deleteTitle')}
        confirmLabel={t('notices.deleteConfirm')}
        busy={busy}
        error={deleteError}
        onClose={() => {
          setDeleting(null)
          setDeleteError(null)
        }}
        onConfirm={async () => {
          if (!deleting) return
          setBusy(true)
          setDeleteError(null)
          try {
            await deleteNotice(deleting.id)
            setNotices(all.filter((n) => n.id !== deleting.id))
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
            <p>
              {deleting.mine
                ? t('notices.deleteMine')
                : t('notices.deleteOther', { name: deleting.author?.name ?? t('notices.formerEmployee') })}
            </p>
            <blockquote className="notice-quote">{deleting.text}</blockquote>
          </>
        )}
      </ConfirmDialog>
    </>
  )
}

function Composer({ onPublished }: { onPublished: (notice: Notice) => void }) {
  const { t } = useTranslation()
  const [text, setText] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [invalid, setInvalid] = useState(false)
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!text.trim()) {
      setInvalid(true)
      document.getElementById('notice-text')?.focus()
      return
    }
    setBusy(true)
    setError(null)
    try {
      onPublished(await publishNotice(text))
      setText('')
      setInvalid(false)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="panel composer" onSubmit={submit} noValidate>
      <ErrorNotice error={error} title={t('notices.notPublished')} />
      <TextArea
        id="notice-text"
        labelText={t('notices.new')}
        helperText={t('notices.newHelp')}
        value={text}
        onChange={(e) => {
          setText(e.target.value)
          if (invalid) setInvalid(false)
        }}
        rows={3}
        maxCount={NOTICE_MAX_LENGTH}
        enableCounter
        invalid={invalid}
        invalidText={t('notices.emptyText')}
      />
      <div className="form-actions">
        <Button type="submit" disabled={busy}>
          {busy ? t('notices.publishing') : t('notices.publish')}
        </Button>
      </div>
    </form>
  )
}

function NoticeItem({ notice, onSaved, onDelete }: { notice: Notice; onSaved: (n: Notice) => void; onDelete: () => void }) {
  const { t, i18n } = useTranslation()
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(notice.text)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  const author = notice.author
  const fromAdmin = author?.administrator ?? false
  const classes = ['notice', fromAdmin && 'notice--admin', notice.mine && 'notice--mine'].filter(Boolean).join(' ')

  async function save(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      onSaved(await editNotice(notice.id, draft))
      setEditing(false)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <li>
      <article className={classes} aria-labelledby={`notice-${notice.id}-author`}>
        <header className="notice__header">
          <span className="avatar" style={{ background: author ? tintFor(author.id) : undefined }} aria-hidden="true">
            {author ? initialOf(author.name) : '·'}
          </span>
          <span className="notice__who">
            <span id={`notice-${notice.id}-author`} className="notice__author">
              {author ? author.name : t('notices.formerEmployee')}
              {notice.mine && <span className="you"> ({t('employees.you')})</span>}
            </span>
            <span className="notice__meta">
              {author && t(`speciality.${author.speciality}`)}
              {author && ' · '}
              <time dateTime={notice.createdAt}>{whenPosted(notice.createdAt, t, i18n.language)}</time>
              {notice.editedAt && <> · {t('notices.edited')}</>}
            </span>
          </span>
          {/* El color nunca es la única señal: el aviso de administración también lo dice con texto */}
          {fromAdmin && (
            <Tag type="blue" size="sm" className="notice__tag">
              {t('notices.fromAdministration')}
            </Tag>
          )}
        </header>

        {editing ? (
          <form onSubmit={save} className="notice__edit" noValidate>
            <ErrorNotice error={error} title={t('form.notSaved')} />
            <TextArea
              id={`notice-${notice.id}-edit`}
              labelText={t('notices.editLabel')}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              rows={3}
              maxCount={NOTICE_MAX_LENGTH}
              enableCounter
            />
            <div className="form-actions">
              <Button kind="secondary" size="sm" type="button" onClick={() => { setEditing(false); setDraft(notice.text) }}>
                {t('common.cancel')}
              </Button>
              <Button size="sm" type="submit" disabled={busy || !draft.trim()}>
                {busy ? t('common.saving') : t('common.save')}
              </Button>
            </div>
          </form>
        ) : (
          <p className="notice__text">{notice.text}</p>
        )}

        {notice.manageable && !editing && (
          <footer className="notice__actions">
            <button type="button" className="text-action" onClick={() => setEditing(true)}>
              {t('common.edit')}
            </button>
            <button type="button" className="text-action text-action--danger" onClick={onDelete}>
              {t('notices.delete')}
            </button>
          </footer>
        )}
      </article>
    </li>
  )
}
