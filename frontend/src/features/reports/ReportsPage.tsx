import { Tag } from '@carbon/react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { listReports, type ReportState, type ReportSummary } from '../../api/reports'
import { formatDate } from '../../shared/dates'
import { whenPosted } from '../../shared/times'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'

type Filter = 'all' | ReportState

// El apartado "Informes" (RF1): los de media estancia y fin de programa de todos los residentes. Se crean
// solos con cada estancia. Pregunta: ¿qué informes están a medias y cuáles se han tocado hace poco?
export function ReportsPage() {
  const { t } = useTranslation()
  const { data, error, loading, reload } = useApi(listReports, [])
  const [filter, setFilter] = useState<Filter>('DRAFT')

  const all = data ?? []
  const counts: Record<Filter, number> = {
    all: all.length,
    DRAFT: all.filter((r) => r.state === 'DRAFT').length,
    CLOSED: all.filter((r) => r.state === 'CLOSED').length,
  }
  const shown = filter === 'all' ? all : all.filter((r) => r.state === filter)

  return (
    <>
      <PageHeader
        title={t('reports.title')}
        subtitle={data ? t('reports.subtitle', { drafts: counts.DRAFT, closed: counts.CLOSED }) : null}
      />

      <section className="list-section" aria-labelledby="reports-title">
        <div className="toolbar">
          <h2 id="reports-title" className="toolbar__title">
            {t('reports.listTitle')}
          </h2>
          <div className="segmented" role="group" aria-label={t('reports.filterLabel')}>
            {(['DRAFT', 'CLOSED', 'all'] as Filter[]).map((f) => (
              <button key={f} type="button" aria-pressed={filter === f} onClick={() => setFilter(f)}>
                {t(`reports.filter.${f}`)} <span className="num">{counts[f]}</span>
              </button>
            ))}
          </div>
        </div>

        {error ? (
          <ErrorNotice error={error} onRetry={reload} />
        ) : loading && !data ? (
          <LoadingRows label={t('common.loading')} />
        ) : shown.length === 0 ? (
          <p className="empty">{t(`reports.empty.${filter}`)}</p>
        ) : (
          <ReportsTable reports={shown} />
        )}
      </section>
    </>
  )
}

function ReportsTable({ reports }: { reports: ReportSummary[] }) {
  const { t, i18n } = useTranslation()
  return (
    <div className="table-wrap">
      <table className="data-table">
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
                  {t(`reports.state.${r.state}`)} · {t('reports.progress', { written: r.writtenSections, total: r.totalSections })}
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
