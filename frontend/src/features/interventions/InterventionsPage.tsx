import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { listFollowUps, type FollowUpSummary } from '../../api/interventions'
import { formatDate } from '../../shared/dates'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'

type Filter = 'all' | 'mine'

// El apartado "Intervenciones" de la memoria. Pregunta: ¿a quién tutelo y cuándo intervine por última vez?
export function InterventionsPage() {
  const { t, i18n } = useTranslation()
  const { data, error, loading, reload } = useApi(listFollowUps, [])
  const [filter, setFilter] = useState<Filter>('all')

  const all = data ?? []
  const counts = { all: all.length, mine: all.filter((s) => s.mine).length }
  const withoutTutor = all.filter((s) => s.tutors.length === 0).length
  const shown = filter === 'mine' ? all.filter((s) => s.mine) : all

  return (
    <>
      <PageHeader
        title={t('interventions.title')}
        subtitle={data ? t('interventions.subtitle', { count: all.length, withoutTutor }) : null}
      />

      <section className="list-section" aria-labelledby="follow-ups-title">
        <div className="toolbar">
          <h2 id="follow-ups-title" className="toolbar__title">
            {t('interventions.listTitle')}
          </h2>
          <div className="segmented" role="group" aria-label={t('interventions.filterLabel')}>
            {(['all', 'mine'] as Filter[]).map((f) => (
              <button key={f} type="button" aria-pressed={filter === f} onClick={() => setFilter(f)}>
                {t(`interventions.filter.${f}`)} <span className="num">{counts[f]}</span>
              </button>
            ))}
          </div>
        </div>

        {error ? (
          <ErrorNotice error={error} onRetry={reload} />
        ) : loading && !data ? (
          <LoadingRows label={t('common.loading')} />
        ) : shown.length === 0 ? (
          <p className="empty">{t(`interventions.empty.${filter}`)}</p>
        ) : (
          <FollowUpsTable summaries={shown} language={i18n.language} />
        )}
      </section>
    </>
  )
}

function FollowUpsTable({ summaries, language }: { summaries: FollowUpSummary[]; language: string }) {
  const { t } = useTranslation()
  return (
    <div className="table-wrap">
      <table className="data-table data-table--stack">
        <caption className="visually-hidden">{t('interventions.tableCaption')}</caption>
        <thead>
          <tr>
            <th scope="col" className="num col-number">{t('patient.number')}</th>
            <th scope="col">{t('patient.fullName')}</th>
            <th scope="col">{t('interventions.tutors')}</th>
            <th scope="col" className="num">{t('interventions.count')}</th>
            <th scope="col" className="num">{t('interventions.last')}</th>
          </tr>
        </thead>
        <tbody>
          {summaries.map((s) => {
            const tutors = s.tutors.length > 0 ? s.tutors.map((tutor) => tutor.name).join(', ') : null
            return (
              <tr key={s.resident.id}>
                <td className="num col-number">{s.resident.number}</td>
                <td className="col-identity">
                  <Link to={`/intervenciones/${s.resident.id}`}>
                    {s.resident.name} {s.resident.surname}
                  </Link>
                  {s.mine && <span className="you"> ({t('interventions.yourTutee')})</span>}
                  {/* En el móvil la fila es de dos líneas: lo que en escritorio son columnas */}
                  <span className="row-sub">
                    {tutors ?? t('interventions.noTutors')} · {t('interventions.countValue', { count: s.interventionCount })} ·{' '}
                    {s.lastIntervention
                      ? t('interventions.lastOn', { date: formatDate(s.lastIntervention, language) })
                      : t('interventions.none')}
                  </span>
                </td>
                <td className="col-wide">{tutors ?? <span className="muted">{t('interventions.noTutors')}</span>}</td>
                <td className="num col-wide">{s.interventionCount}</td>
                <td className="num col-wide">
                  {s.lastIntervention ? formatDate(s.lastIntervention, language) : <span className="dash">—</span>}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
