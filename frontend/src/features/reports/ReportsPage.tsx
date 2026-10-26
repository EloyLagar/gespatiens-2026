import { Button, Search, Select, SelectItem, Tag } from '@carbon/react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { listReports, type ReportState, type ReportSummary, type ReportType } from '../../api/reports'
import { formatDate } from '../../shared/dates'
import { whenPosted } from '../../shared/times'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { matches } from '../patients/search'

type Filter = 'all' | ReportState
type TypeFilter = 'all' | ReportType
// La estancia en curso de quien sigue en el centro es el trabajo del día a día; las terminadas, el archivo
type StayFilter = 'current' | 'finished' | 'all'

const PAGE = 25

// El apartado "Informes" (RF1): los de media estancia y fin de programa de todos los residentes. Se crean
// solos con cada estancia. Pregunta: ¿qué informes están a medias y cuáles se han tocado hace poco?
// Los filtros van en la URL: el panel de control enlaza aquí y volver atrás conserva la búsqueda
export function ReportsPage() {
  const { t } = useTranslation()
  const { data, error, loading, reload } = useApi(listReports, [])
  const [params, setParams] = useSearchParams()
  const filter = (params.get('estado') as Filter | null) ?? 'DRAFT'
  const type = (params.get('tipo') as TypeFilter | null) ?? 'all'
  const stay = (params.get('estancia') as StayFilter | null) ?? 'current'
  const query = params.get('q') ?? ''
  const limit = Number(params.get('ver') ?? PAGE)

  // Cambiar un filtro vuelve a la primera página. replace: escribir en el buscador no llena el historial
  const set = (key: string, value: string, fallback: string) => {
    const next = new URLSearchParams(params)
    if (value === fallback) next.delete(key)
    else next.set(key, value)
    next.delete('ver')
    setParams(next, { replace: true })
  }

  const all = data ?? []
  const filtered = all.filter(
    (r) =>
      (type === 'all' || r.type === type) &&
      (stay === 'all' || (stay === 'current') === (r.stayEnd === null)) &&
      matches(r.resident, query),
  )
  const counts: Record<Filter, number> = {
    all: filtered.length,
    DRAFT: filtered.filter((r) => r.state === 'DRAFT').length,
    CLOSED: filtered.filter((r) => r.state === 'CLOSED').length,
  }
  const shown = filter === 'all' ? filtered : filtered.filter((r) => r.state === filter)
  const narrowed = query !== '' || type !== 'all' || stay !== 'all'

  return (
    <>
      <PageHeader
        title={t('reports.title')}
        subtitle={
          data
            ? t('reports.subtitle', {
                drafts: all.filter((r) => r.state === 'DRAFT').length,
                closed: all.filter((r) => r.state === 'CLOSED').length,
              })
            : null
        }
      />

      <section className="list-section" aria-labelledby="reports-title">
        <div className="toolbar">
          <h2 id="reports-title" className="toolbar__title">
            {t('reports.listTitle')}
          </h2>
          <div className="segmented" role="group" aria-label={t('reports.filterLabel')}>
            {(['DRAFT', 'CLOSED', 'all'] as Filter[]).map((f) => (
              <button key={f} type="button" aria-pressed={filter === f} onClick={() => set('estado', f, 'DRAFT')}>
                {t(`reports.filter.${f}`)} <span className="num">{counts[f]}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="filters">
          <Search
            size="md"
            labelText={t('reports.search')}
            placeholder={t('reports.searchPlaceholder')}
            value={query}
            onChange={(e) => set('q', e.target.value, '')}
            closeButtonLabelText={t('common.clearSearch')}
          />
          <Select id="reports-type" labelText={t('reports.typeFilter')} size="md" value={type} onChange={(e) => set('tipo', e.target.value, 'all')}>
            <SelectItem value="all" text={t('reports.typeAll')} />
            <SelectItem value="MID_STAY" text={t('reports.type.MID_STAY')} />
            <SelectItem value="FINAL" text={t('reports.type.FINAL')} />
          </Select>
          <Select id="reports-stay" labelText={t('reports.stayFilter')} size="md" value={stay} onChange={(e) => set('estancia', e.target.value, 'current')}>
            <SelectItem value="current" text={t('reports.stayCurrent')} />
            <SelectItem value="finished" text={t('reports.stayFinished')} />
            <SelectItem value="all" text={t('reports.stayAll')} />
          </Select>
        </div>

        {error ? (
          <ErrorNotice error={error} onRetry={reload} />
        ) : loading && !data ? (
          <LoadingRows label={t('common.loading')} />
        ) : shown.length === 0 ? (
          <p className="empty">{narrowed && all.length > 0 ? t('reports.noMatches') : t(`reports.empty.${filter}`)}</p>
        ) : (
          <>
            <p className="list-count num" aria-live="polite">
              {t('reports.showing', { shown: Math.min(limit, shown.length), count: shown.length })}
            </p>
            <ReportsTable reports={shown.slice(0, limit)} />
            {shown.length > limit && (
              <div className="list-more">
                <Button
                  kind="tertiary"
                  size="md"
                  onClick={() => {
                    const next = new URLSearchParams(params)
                    next.set('ver', String(limit + PAGE))
                    setParams(next, { replace: true })
                  }}
                >
                  {t('reports.showMore', { count: Math.min(PAGE, shown.length - limit) })}
                </Button>
              </div>
            )}
          </>
        )}
      </section>
    </>
  )
}

function ReportsTable({ reports }: { reports: ReportSummary[] }) {
  const { t, i18n } = useTranslation()
  return (
    <div className="table-wrap">
      <table className="data-table data-table--stack">
        <caption className="visually-hidden">{t('reports.tableCaption')}</caption>
        <thead>
          <tr>
            <th scope="col" className="num col-number">{t('patient.number')}</th>
            <th scope="col">{t('reports.col.report')}</th>
            <th scope="col">{t('reports.col.state')}</th>
            <th scope="col" className="num">{t('reports.col.progress')}</th>
            <th scope="col">{t('reports.col.updated')}</th>
          </tr>
        </thead>
        <tbody>
          {reports.map((r) => (
            <tr key={r.id}>
              <td className="num col-number">{r.resident.number}</td>
              <td className="col-identity">
                <Link to={`/informes/${r.id}`}>
                  {r.resident.name} {r.resident.surname}
                </Link>
                <span className="row-meta">
                  {t(`reports.type.${r.type}`)} ·{' '}
                  {t('reports.stay', {
                    from: formatDate(r.stayStart, i18n.language),
                    to: r.stayEnd ? formatDate(r.stayEnd, i18n.language) : t('reports.stayOngoing'),
                  })}
                </span>
                {/* En el móvil la fila es de dos líneas: lo que en escritorio son columnas */}
                <span className="row-sub">
                  {t(`reports.state.${r.state}`)} · {t('reports.progress', { written: r.writtenSections, total: r.totalSections })} ·{' '}
                  {t('reports.updatedWhen', { when: whenPosted(r.lastUpdate, t, i18n.language) })}
                </span>
              </td>
              <td className="col-wide">
                <ReportStateTag state={r.state} />
              </td>
              <td className="num col-wide">
                {t('reports.progress', { written: r.writtenSections, total: r.totalSections })}
              </td>
              <td className="col-wide">
                <time dateTime={r.lastUpdate}>{whenPosted(r.lastUpdate, t, i18n.language)}</time>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function ReportStateTag({ state }: { state: ReportState }) {
  const { t } = useTranslation()
  return (
    <Tag type={state === 'DRAFT' ? 'blue' : 'gray'} size="sm">
      {t(`reports.state.${state}`)}
    </Tag>
  )
}
