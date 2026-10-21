import { TextInput } from '@carbon/react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { getDashboard, type Dashboard } from '../../api/dashboard'
import { todayIso } from '../../shared/dates'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'

// Memoria final, 5.1: "un panel de administración al que solo esta pudiese acceder y ver distinta información,
// por ejemplo: la cantidad de turnos que realizan distintos trabajadores durante un mes, la cantidad de avisos
// por semana, etc." Las cifras de arriba son las de hoy; los gráficos, los del mes elegido
export function DashboardPage() {
  const { t } = useTranslation()
  const [month, setMonth] = useState(todayIso().slice(0, 7))
  const { data, error, loading, reload } = useApi(() => getDashboard(month), [month])

  return (
    <>
      <PageHeader title={t('dashboard.title')} subtitle={t('dashboard.subtitle')} />

      <div className="dashboard-filters">
        <TextInput id="dashboard-month" type="month" labelText={t('dashboard.month')} value={month}
          onChange={(e) => e.target.value && setMonth(e.target.value)} />
      </div>

      {error ? (
        <ErrorNotice error={error} onRetry={reload} />
      ) : loading && !data ? (
        <LoadingRows rows={6} label={t('common.loading')} />
      ) : data && (
        <>
          <Figures data={data} />
          <div className="dashboard-charts">
            <ShiftsChart data={data} />
            <NoticesChart data={data} />
          </div>
        </>
      )}
    </>
  )
}

// La fila de cifras: plazas (con su medidor), residentes sin sus dos tutores, informes a medias e intervenciones
function Figures({ data }: { data: Dashboard }) {
  const { t } = useTranslation()
  const occupancy = data.occupiedPlaces / data.totalPlaces
  return (
    <section className="stat-row" aria-label={t('dashboard.figures')}>
      <div className="stat">
        <p className="stat__label">{t('dashboard.places')}</p>
        <p className="stat__value">
          {data.occupiedPlaces}
          <span className="stat__unit"> / {data.totalPlaces}</span>
        </p>
        <div className="meter" role="meter" aria-valuemin={0} aria-valuemax={data.totalPlaces} aria-valuenow={data.occupiedPlaces}
          aria-label={t('dashboard.places')}>
          <span className="meter__fill" style={{ width: `${occupancy * 100}%` }} />
        </div>
        <p className="stat__note">{t('dashboard.freePlaces', { count: data.totalPlaces - data.occupiedPlaces })}</p>
      </div>
      <div className="stat">
        <p className="stat__label">{t('dashboard.withoutTutors')}</p>
        <p className="stat__value">{data.residentsWithoutBothTutors}</p>
        <Link className="stat__note" to="/intervenciones">{t('dashboard.goTutors')}</Link>
      </div>
      <div className="stat">
        <p className="stat__label">{t('dashboard.reportsInProgress')}</p>
        <p className="stat__value">{data.reportsInProgress}</p>
        <Link className="stat__note" to="/informes">{t('dashboard.goReports')}</Link>
      </div>
      <div className="stat">
        <p className="stat__label">{t('dashboard.interventions')}</p>
        <p className="stat__value">{data.interventions}</p>
        <p className="stat__note">{t('dashboard.inMonth')}</p>
      </div>
    </section>
  )
}

// Turnos por trabajador: barras horizontales (los nombres son largos), una sola serie, valor en la punta.
// El desglose por parte del día va en el tooltip y en la tabla
function ShiftsChart({ data }: { data: Dashboard }) {
  const { t } = useTranslation()
  const max = Math.max(1, ...data.shifts.map((s) => s.total))
  return (
    <section className="panel chart" aria-labelledby="chart-shifts-title">
      <h2 id="chart-shifts-title">{t('dashboard.shiftsTitle')}</h2>
      <p className="chart__subtitle">{t('dashboard.shiftsSubtitle')}</p>
      {data.shifts.length === 0 ? (
        <p className="empty">{t('dashboard.noShifts')}</p>
      ) : (
        <>
          <ul className="hbars">
            {data.shifts.map((s) => {
              const detail = t('dashboard.shiftsDetail', { morning: s.morning, afternoon: s.afternoon, night: s.night })
              return (
                <li key={s.employeeId} className="hbars__row" tabIndex={0} aria-label={`${s.name}: ${s.total} · ${detail}`}>
                  <span className="hbars__name">{s.name}</span>
                  <span className="hbars__track">
                    <span className="hbars__bar" style={{ width: `${(s.total / max) * 100}%` }} />
                    <span className="hbars__value num">{s.total}</span>
                  </span>
                  <span className="chart-tip" role="tooltip">{detail}</span>
                </li>
              )
            })}
          </ul>
          <details className="chart__table">
            <summary>{t('dashboard.asTable')}</summary>
            <table className="data-table">
              <caption className="visually-hidden">{t('dashboard.shiftsTitle')}</caption>
              <thead>
                <tr>
                  <th scope="col">{t('dashboard.employee')}</th>
                  <th scope="col" className="num">{t('diary.part.MORNING')}</th>
                  <th scope="col" className="num">{t('diary.part.AFTERNOON')}</th>
                  <th scope="col" className="num">{t('diary.part.NIGHT')}</th>
                  <th scope="col" className="num">{t('dashboard.total')}</th>
                </tr>
              </thead>
              <tbody>
                {data.shifts.map((s) => (
                  <tr key={s.employeeId}>
                    <th scope="row">{s.name}</th>
                    <td className="num">{s.morning}</td>
                    <td className="num">{s.afternoon}</td>
                    <td className="num">{s.night}</td>
                    <td className="num">{s.total}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </details>
        </>
      )}
    </section>
  )
}

// Avisos por semana: columnas de lunes a domingo, una sola serie, valor sobre cada columna
function NoticesChart({ data }: { data: Dashboard }) {
  const { t, i18n } = useTranslation()
  const max = Math.max(1, ...data.noticesByWeek.map((w) => w.notices))
  const day = new Intl.DateTimeFormat(i18n.language === 'en' ? 'en-GB' : 'es-ES', { day: 'numeric', month: 'short' })
  const label = (iso: string) => {
    const [y, m, d] = iso.split('-').map(Number)
    return day.format(new Date(y, m - 1, d))
  }
  return (
    <section className="panel chart" aria-labelledby="chart-notices-title">
      <h2 id="chart-notices-title">{t('dashboard.noticesTitle')}</h2>
      <p className="chart__subtitle">{t('dashboard.noticesSubtitle')}</p>
      <ol className="columns">
        {data.noticesByWeek.map((w) => (
          <li key={w.monday} className="columns__item" tabIndex={0}
            aria-label={t('dashboard.weekOf', { date: label(w.monday), count: w.notices })}>
            <span className="columns__value num">{w.notices}</span>
            <span className="columns__track">
              <span className="columns__bar" style={{ height: `${(w.notices / max) * 100}%` }} />
            </span>
            <span className="columns__label">{label(w.monday)}</span>
            <span className="chart-tip" role="tooltip">{t('dashboard.weekOf', { date: label(w.monday), count: w.notices })}</span>
          </li>
        ))}
      </ol>
    </section>
  )
}
