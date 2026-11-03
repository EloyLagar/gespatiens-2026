import { Button, Select, SelectItem, Tag, TextInput } from '@carbon/react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { searchAccessLog, type AccessEntry, type AccessOutcome } from '../../api/audit'
import { listUsers } from '../../api/users'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { LogTabs } from './LogTabs'

// Registro de accesos (ENS op.acc, op.exp.8). Pregunta: ¿quién ha entrado, desde dónde, y quién lo ha intentado
// sin conseguirlo? Se conserva 2 años. Los filtros van en la URL, como en el registro de cambios
const RESULTS: Record<string, AccessOutcome[]> = {
  '': [],
  ok: ['SUCCESS'],
  failed: ['FAILURE', 'LOCKED', 'BLOCKED'],
  locked: ['LOCKED'],
}

const TAG: Record<AccessOutcome, 'green' | 'warm-gray' | 'red' | 'magenta'> = {
  SUCCESS: 'green',
  FAILURE: 'warm-gray',
  LOCKED: 'red',
  BLOCKED: 'magenta',
}

export function AccessLogPage() {
  const { t, i18n } = useTranslation()
  const [params, setParams] = useSearchParams()
  const userId = params.get('empleado') ?? ''
  const result = params.get('resultado') ?? ''
  const from = params.get('desde') ?? ''
  const to = params.get('hasta') ?? ''
  const page = Number(params.get('pagina') ?? 0)
  const outcomes = RESULTS[result] ?? []

  const { data, error, loading, reload } = useApi(
    () => searchAccessLog({ userId, outcomes, from, to, page }),
    [userId, result, from, to, page],
  )
  const users = useApi(listUsers, [])

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

  const when = new Intl.DateTimeFormat(i18n.language === 'en' ? 'en-GB' : 'es-ES', { dateStyle: 'short', timeStyle: 'medium' })

  return (
    <>
      <PageHeader title={t('access.title')} subtitle={t('access.subtitle')} />
      <LogTabs />

      <div className="filters filters--four">
        <Select id="access-employee" labelText={t('audit.employee')} size="md" value={userId} onChange={(e) => set('empleado', e.target.value)}>
          <SelectItem value="" text={t('audit.anyEmployee')} />
          {(users.data ?? []).map((u) => (
            <SelectItem key={u.id} value={u.id} text={u.name} />
          ))}
        </Select>
        <Select id="access-result" labelText={t('access.result')} size="md" value={result} onChange={(e) => set('resultado', e.target.value)}>
          <SelectItem value="" text={t('access.anyResult')} />
          <SelectItem value="ok" text={t('access.onlyOk')} />
          <SelectItem value="failed" text={t('access.onlyFailed')} />
          <SelectItem value="locked" text={t('access.onlyLocked')} />
        </Select>
        <TextInput id="access-from" type="date" labelText={t('audit.from')} size="md" value={from} max={to || undefined}
          onChange={(e) => set('desde', e.target.value)} />
        <TextInput id="access-to" type="date" labelText={t('audit.to')} size="md" value={to} min={from || undefined}
          onChange={(e) => set('hasta', e.target.value)} />
      </div>

      {error ? (
        <ErrorNotice error={error} onRetry={reload} />
      ) : loading && !data ? (
        <LoadingRows label={t('common.loading')} />
      ) : data && data.entries.length === 0 ? (
        <p className="empty">{t('access.empty')}</p>
      ) : data && (
        <>
          <p className="list-count num" aria-live="polite">{t('access.count', { count: data.totalEntries })}</p>
          <div className="table-wrap">
            <table className="data-table data-table--stack">
              <caption className="visually-hidden">{t('access.title')}</caption>
              <thead>
                <tr>
                  <th scope="col">{t('audit.when')}</th>
                  <th scope="col">{t('access.who')}</th>
                  <th scope="col">{t('access.result')}</th>
                  <th scope="col">{t('access.from')}</th>
                </tr>
              </thead>
              <tbody>
                {data.entries.map((e) => (
                  <tr key={e.id}>
                    <td className="num col-wide">
                      <time dateTime={e.occurredAt}>{when.format(new Date(e.occurredAt))}</time>
                    </td>
                    <td className="col-identity">
                      <WhoCell entry={e} />
                      {/* En el móvil la fila es de dos líneas: lo que en escritorio son columnas */}
                      <span className="row-sub">
                        <OutcomeTag outcome={e.outcome} /> <OriginCell entry={e} />
                      </span>
                      <span className="row-sub">
                        <time dateTime={e.occurredAt}>{when.format(new Date(e.occurredAt))}</time>
                      </span>
                    </td>
                    <td className="col-wide"><OutcomeTag outcome={e.outcome} /></td>
                    <td className="col-wide"><OriginCell entry={e} /></td>
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

// El nombre si el email es de alguien de la plantilla; si no, el email tal cual se escribió
function WhoCell({ entry }: { entry: AccessEntry }) {
  const { t } = useTranslation()
  if (!entry.employee) {
    return (
      <>
        <span className="access-email">{entry.email}</span>
        <span className="row-sub row-sub--always">{t('access.unknownEmail')}</span>
      </>
    )
  }
  return (
    <>
      {entry.employee.name ?? <span className="dash">{t('audit.formerEmployee')}</span>}
      <span className="row-sub row-sub--always access-email">{entry.email}</span>
    </>
  )
}

function OutcomeTag({ outcome }: { outcome: AccessOutcome }) {
  const { t } = useTranslation()
  return <Tag type={TAG[outcome]} size="sm">{t(`access.outcome.${outcome}`)}</Tag>
}

function OriginCell({ entry }: { entry: AccessEntry }) {
  const { t } = useTranslation()
  const browser = describeBrowser(entry.userAgent)
  if (!entry.ipAddress && !browser) return <span className="dash">{t('access.unknownOrigin')}</span>
  return (
    <span className="num">
      {entry.ipAddress ?? t('access.unknownIp')}
      {browser && <> · {browser}</>}
    </span>
  )
}

// Del User-Agent basta con el navegador y el sistema para reconocer un equipo: "Firefox en Windows"
export function describeBrowser(userAgent: string | null): string | null {
  if (!userAgent) return null
  const browser =
    /Edg\//.test(userAgent) ? 'Edge'
      : /OPR\/|Opera/.test(userAgent) ? 'Opera'
        : /Firefox\//.test(userAgent) ? 'Firefox'
          : /Chrome\//.test(userAgent) ? 'Chrome'
            : /Safari\//.test(userAgent) ? 'Safari'
              : null
  const system =
    /Windows/.test(userAgent) ? 'Windows'
      : /Android/.test(userAgent) ? 'Android'
        : /iPhone|iPad/.test(userAgent) ? 'iOS'
          : /Mac OS X/.test(userAgent) ? 'macOS'
            : /Linux/.test(userAgent) ? 'Linux'
              : null
  if (browser && system) return `${browser} · ${system}`
  return browser ?? system ?? userAgent.slice(0, 40)
}
