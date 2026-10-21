import { Button, Search } from '@carbon/react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { listPatients } from '../../api/patients'
import type { Patient } from '../../api/types'
import { daysBetween, formatDate } from '../../shared/dates'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { bedsOf, nextFreeNumber } from './beds'
import { ReadmitDialog } from './ReadmitDialog'
import { useAuth } from '../../auth/AuthContext'
import { matches } from './search'

// Pregunta de la pantalla: ¿quién ha pasado por el centro y puede volver?
export function FormerResidentsPage() {
  const { t, i18n } = useTranslation()
  // Memoria final: reincorporar es de la administración
  const { isAdmin } = useAuth()
  const navigate = useNavigate()
  const former = useApi(() => listPatients('FORMER'), [])
  // Las plazas libres deciden si se puede reincorporar a alguien (RF10) y con qué número
  const active = useApi(() => listPatients('ACTIVE'), [])
  const [query, setQuery] = useState('')
  const [readmitting, setReadmitting] = useState<Patient | null>(null)

  const nextNumber = useMemo(() => (active.data ? nextFreeNumber(bedsOf(active.data)) : null), [active.data])
  const patients = former.data ?? []
  const shown = patients.filter((p) => matches(p, query))
  const centreFull = active.data !== null && nextNumber === null

  return (
    <>
      <PageHeader
        title={t('former.title')}
        subtitle={former.data ? t('former.subtitle', { count: patients.length }) : null}
      />

      {centreFull && <p className="notice">{t('former.fullNotice')}</p>}

      <section className="list-section" aria-labelledby="former-list-title">
        <div className="toolbar">
          <h2 id="former-list-title" className="toolbar__title">
            {t('former.listTitle')}
          </h2>
          <Search
            size="md"
            labelText={t('residents.search')}
            placeholder={t('former.searchPlaceholder')}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            closeButtonLabelText={t('common.clearSearch')}
          />
          {former.data && (
            <p className="toolbar__count num" aria-live="polite">
              {t('former.count', { count: shown.length })}
            </p>
          )}
        </div>

        {former.error ? (
          <ErrorNotice error={former.error} onRetry={former.reload} />
        ) : former.loading && !former.data ? (
          <LoadingRows label={t('common.loading')} />
        ) : patients.length === 0 ? (
          <p className="empty">{t('former.empty')}</p>
        ) : shown.length === 0 ? (
          <p className="empty">{t('residents.noMatch', { query })}</p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <caption className="visually-hidden">{t('former.tableCaption')}</caption>
              <thead>
                <tr>
                  <th scope="col">{t('patient.fullName')}</th>
                  <th scope="col">{t('patient.dni')}</th>
                  <th scope="col" className="num">{t('patient.entryDate')}</th>
                  <th scope="col" className="num">{t('patient.exitDate')}</th>
                  <th scope="col" className="num">{t('patient.stay')}</th>
                  {isAdmin && (
                    <th scope="col" className="col-action">
                      <span className="visually-hidden">{t('common.actions')}</span>
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {shown.map((p) => (
                  <tr key={p.id}>
                    <td className="col-identity">
                      <Link to={`/residentes/${p.id}`}>
                        {p.name} {p.surname}
                      </Link>
                      <span className="row-sub">
                        {p.dni} · {t('former.left', { date: formatDate(p.exitDate, i18n.language) })}
                      </span>
                    </td>
                    <td className="col-wide">{p.dni}</td>
                    <td className="num col-wide">{formatDate(p.entryDate, i18n.language)}</td>
                    <td className="num col-wide">{formatDate(p.exitDate, i18n.language)}</td>
                    <td className="num col-wide">
                      {p.exitDate ? (
                        <>
                          {daysBetween(p.entryDate, p.exitDate)}{' '}
                          <span className="unit">
                            {t('common.dayUnit', { count: daysBetween(p.entryDate, p.exitDate) })}
                          </span>
                        </>
                      ) : (
                        <span className="dash">—</span>
                      )}
                    </td>
                    {isAdmin && (
                      <td className="col-action">
                        <Button
                          kind="ghost"
                          size="sm"
                          disabled={centreFull}
                          onClick={() => setReadmitting(p)}
                          aria-label={t('former.readmitWho', { name: `${p.name} ${p.surname}` })}
                        >
                          {t('former.readmit')}
                        </Button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <ReadmitDialog
        key={readmitting?.id}
        patient={readmitting}
        nextNumber={nextNumber}
        onClose={() => setReadmitting(null)}
        onDone={(patient) => navigate(`/residentes/${patient.id}`, { state: { flash: 'readmitted' } })}
      />
    </>
  )
}
