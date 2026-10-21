import { Button, Search } from '@carbon/react'
import { Add } from '@carbon/icons-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { listPatients } from '../../api/patients'
import { useAuth } from '../../auth/AuthContext'
import { MAX_RESIDENTS, type Patient } from '../../api/types'
import { ageOn, daysBetween, formatDate } from '../../shared/dates'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { BedStrip } from './BedStrip'
import { bedsOf, nextFreeNumber } from './beds'
import { matches } from './search'

// Pregunta de la pantalla: ¿quién está en el centro y cuántas plazas quedan?
export function ResidentsPage() {
  const { t, i18n } = useTranslation()
  const { isAdmin } = useAuth()
  const navigate = useNavigate()
  const { data, error, loading, reload } = useApi(() => listPatients('ACTIVE'), [])
  const [query, setQuery] = useState('')

  const patients = useMemo(() => data ?? [], [data])
  const beds = useMemo(() => bedsOf(patients), [patients])
  const next = nextFreeNumber(beds)
  const free = MAX_RESIDENTS - patients.length
  const shown = patients.filter((p) => matches(p, query))

  const subtitle = data
    ? next === null
      ? t('residents.subtitleFull', { max: MAX_RESIDENTS })
      : t('residents.subtitle', { count: patients.length, max: MAX_RESIDENTS, next })
    : null

  return (
    <>
      <PageHeader
        title={t('residents.title')}
        subtitle={subtitle}
        // Memoria final: las altas las da la administración
        actions={isAdmin && (
          <Button
            renderIcon={Add}
            onClick={() => navigate('/residentes/nuevo')}
            disabled={!data || next === null}
            // RF10: con 32 residentes el alta está cerrada, y el botón dice por qué
            title={next === null ? t('residents.fullHint') : undefined}
          >
            {t('residents.admit')}
          </Button>
        )}
      />

      <section className="panel" aria-labelledby="beds-title">
        <div className="panel__header">
          <h2 id="beds-title">{t('beds.title')}</h2>
          {data && (
            <p className="panel__meta num">
              {t('beds.summary', { taken: patients.length, free, max: MAX_RESIDENTS })}
            </p>
          )}
        </div>
        {error ? (
          <ErrorNotice error={error} onRetry={reload} />
        ) : data ? (
          <BedStrip beds={beds} next={next} />
        ) : (
          <LoadingRows rows={2} label={t('common.loading')} />
        )}
      </section>

      <section className="list-section" aria-labelledby="list-title">
        <div className="toolbar">
          <h2 id="list-title" className="toolbar__title">
            {t('residents.listTitle')}
          </h2>
          <Search
            size="md"
            labelText={t('residents.search')}
            placeholder={t('residents.searchPlaceholder')}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            closeButtonLabelText={t('common.clearSearch')}
          />
          {data && (
            <p className="toolbar__count num" aria-live="polite">
              {t('residents.count', { count: shown.length })}
            </p>
          )}
        </div>

        {loading && !data ? (
          <LoadingRows label={t('common.loading')} />
        ) : data && patients.length === 0 ? (
          <p className="empty">{t('residents.empty')}</p>
        ) : data && shown.length === 0 ? (
          <p className="empty">{t('residents.noMatch', { query })}</p>
        ) : data ? (
          <ResidentsTable patients={shown} language={i18n.language} />
        ) : null}
      </section>
    </>
  )
}

function ResidentsTable({ patients, language }: { patients: Patient[]; language: string }) {
  const { t } = useTranslation()
  return (
    <div className="table-wrap">
      <table className="data-table">
        <caption className="visually-hidden">{t('residents.tableCaption')}</caption>
        <thead>
          <tr>
            <th scope="col" className="num col-number">{t('patient.number')}</th>
            <th scope="col">{t('patient.fullName')}</th>
            <th scope="col">{t('patient.dni')}</th>
            <th scope="col" className="num">{t('patient.age')}</th>
            <th scope="col" className="num">{t('patient.entryDate')}</th>
            <th scope="col" className="num">{t('patient.daysIn')}</th>
          </tr>
        </thead>
        <tbody>
          {patients.map((p) => (
            <tr key={p.id}>
              <td className="num col-number">{p.number}</td>
              <td className="col-identity">
                <Link to={`/residentes/${p.id}`}>
                  {p.name} {p.surname}
                </Link>
                {/* En el móvil la fila es de dos líneas: lo que en escritorio son columnas */}
                <span className="row-sub">
                  {p.dni} · {t('patient.ageValue', { count: ageOn(p.birthDate) })} ·{' '}
                  {t('patient.since', { date: formatDate(p.entryDate, language) })}
                </span>
              </td>
              <td className="col-wide">{p.dni}</td>
              <td className="num col-wide">{ageOn(p.birthDate)}</td>
              <td className="num col-wide">{formatDate(p.entryDate, language)}</td>
              <td className="num col-wide">
                {daysBetween(p.entryDate)}{' '}
                <span className="unit">{t('common.dayUnit', { count: daysBetween(p.entryDate) })}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
