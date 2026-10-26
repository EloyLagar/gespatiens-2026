import { Button, Select, SelectItem, TextInput } from '@carbon/react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { searchAudit, type AuditEntry } from '../../api/audit'
import { listPatients } from '../../api/patients'
import { listUsers } from '../../api/users'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'

// Registro de cambios (RNF7, RGPD). Pregunta: ¿quién ha cambiado los datos de este residente, o qué ha
// cambiado esta persona? Se conserva 2 años. Los filtros van en la URL: la ficha del residente enlaza aquí con el suyo puesto
export function AuditPage() {
  const { t, i18n } = useTranslation()
  const [params, setParams] = useSearchParams()
  const patientId = params.get('residente') ?? ''
  const actorId = params.get('empleado') ?? ''
  const from = params.get('desde') ?? ''
  const to = params.get('hasta') ?? ''
  const page = Number(params.get('pagina') ?? 0)

  const { data, error, loading, reload } = useApi(
    () => searchAudit({ patientId, actorId, from, to, page }),
    [patientId, actorId, from, to, page],
  )
  const users = useApi(listUsers, [])
  const residents = useApi(async () => {
    const [active, former] = await Promise.all([listPatients('ACTIVE'), listPatients('FORMER')])
    return [...active, ...former].sort((a, b) => `${a.surname} ${a.name}`.localeCompare(`${b.surname} ${b.name}`, 'es'))
  }, [])

  const set = (key: string, value: string) => {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    next.delete('pagina')
    setParams(next, { replace: true })
  }
  const goTo = (p: number) => {
    const next = new URLSearchParams(params)
    if (p > 0) next.set('pagina', String(p))
    else next.delete('pagina')
    setParams(next)
  }

  const when = new Intl.DateTimeFormat(i18n.language === 'en' ? 'en-GB' : 'es-ES', { dateStyle: 'short', timeStyle: 'short' })

  return (
    <>
      <PageHeader title={t('audit.title')} subtitle={t('audit.subtitle')} />

      <div className="filters filters--four">
        <Select id="audit-resident" labelText={t('audit.resident')} size="md" value={patientId} onChange={(e) => set('residente', e.target.value)}>
          <SelectItem value="" text={t('audit.anyResident')} />
          {(residents.data ?? []).map((p) => (
            <SelectItem key={p.id} value={p.id} text={`${p.surname}, ${p.name}${p.number ? ` (${p.number})` : ''}`} />
          ))}
        </Select>
        <Select id="audit-actor" labelText={t('audit.employee')} size="md" value={actorId} onChange={(e) => set('empleado', e.target.value)}>
          <SelectItem value="" text={t('audit.anyEmployee')} />
          {(users.data ?? []).map((u) => (
            <SelectItem key={u.id} value={u.id} text={u.name} />
          ))}
        </Select>
        <TextInput id="audit-from" type="date" labelText={t('audit.from')} size="md" value={from} max={to || undefined}
          onChange={(e) => set('desde', e.target.value)} />
        <TextInput id="audit-to" type="date" labelText={t('audit.to')} size="md" value={to} min={from || undefined}
          onChange={(e) => set('hasta', e.target.value)} />
      </div>

      {error ? (
        <ErrorNotice error={error} onRetry={reload} />
      ) : loading && !data ? (
        <LoadingRows label={t('common.loading')} />
      ) : data && data.entries.length === 0 ? (
        <p className="empty">{t('audit.empty')}</p>
      ) : data && (
        <>
          <p className="list-count num" aria-live="polite">{t('audit.count', { count: data.totalEntries })}</p>
          <div className="table-wrap">
            <table className="data-table data-table--stack">
              <caption className="visually-hidden">{t('audit.title')}</caption>
              <thead>
                <tr>
                  <th scope="col">{t('audit.when')}</th>
                  <th scope="col">{t('audit.employee')}</th>
                  <th scope="col">{t('audit.what')}</th>
                  <th scope="col">{t('audit.resident')}</th>
                </tr>
              </thead>
              <tbody>
                {data.entries.map((e) => (
                  <tr key={e.id}>
                    <td className="num col-wide">
                      <time dateTime={e.occurredAt}>{when.format(new Date(e.occurredAt))}</time>
                    </td>
                    <td className="col-identity">
                      {e.actor.name ?? <span className="dash">{t('audit.formerEmployee')}</span>}
                      {/* En el móvil la fila es de dos líneas: lo que en escritorio son columnas */}
                      <span className="row-sub">
                        <ActionCell entry={e} /> · <ResidentCell entry={e} />
                      </span>
                      <span className="row-sub">
                        <time dateTime={e.occurredAt}>{when.format(new Date(e.occurredAt))}</time>
                      </span>
                    </td>
                    <td className="col-wide"><ActionCell entry={e} /></td>
                    <td className="col-wide"><ResidentCell entry={e} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {data.totalPages > 1 && (
            <nav className="pager" aria-label={t('audit.pages')}>
              <Button kind="tertiary" size="md" disabled={page === 0} onClick={() => goTo(page - 1)}>
                {t('audit.newer')}
              </Button>
              <span className="num">{t('audit.page', { page: page + 1, total: data.totalPages })}</span>
              <Button kind="tertiary" size="md" disabled={page + 1 >= data.totalPages} onClick={() => goTo(page + 1)}>
                {t('audit.older')}
              </Button>
            </nav>
          )}
        </>
      )}
    </>
  )
}

function ActionCell({ entry }: { entry: AuditEntry }) {
  const { t } = useTranslation()
  const label = t(`audit.action.${entry.action}`)
  // El informe concreto, si sigue existiendo, se abre desde aquí
  if (entry.resourceId && entry.action.endsWith('REPORT')) {
    return <Link to={`/informes/${entry.resourceId}`}>{label}</Link>
  }
  return <>{label}</>
}

function ResidentCell({ entry }: { entry: AuditEntry }) {
  const { t } = useTranslation()
  const r = entry.resident
  if (!r) return <span className="dash">—</span>
  if (!r.name) return <span className="dash">{t('audit.deletedResident')}</span>
  return (
    <Link to={`/residentes/${r.id}`}>
      {r.name} {r.surname}
      {r.number ? ` (${r.number})` : ''}
    </Link>
  )
}
