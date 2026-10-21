import { Button } from '@carbon/react'
import { Download, View } from '@carbon/icons-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { getResidentReports, REPORT_TYPES, residentReportsPdf, type ReportSummary, type ReportType } from '../../api/reports'
import { formatDate } from '../../shared/dates'
import { previewFile, saveFile } from '../../shared/files'
import { ErrorNotice, LoadingRows } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { ReportStateTag } from './ReportsPage'

// En la ficha: "desde la ficha de cada usuario se debe poder acceder a modificar los informes". Un informe de
// cada tipo por estancia, de la más reciente a la más antigua, y los PDF que los reúnen (RF2)
export function ResidentReportsPanel({ patientId, active }: { patientId: string; active: boolean }) {
  const { t, i18n } = useTranslation()
  // active entra en las dependencias: tras una baja o un reingreso cambian las estancias
  const { data, error, loading, reload } = useApi(() => getResidentReports(patientId), [patientId, active])
  const [pdfBusy, setPdfBusy] = useState(false)
  const [pdfError, setPdfError] = useState<unknown>(null)

  async function pdf(type: ReportType | null, preview: boolean) {
    setPdfBusy(true)
    setPdfError(null)
    try {
      if (preview) await previewFile(() => residentReportsPdf(patientId, type, true))
      else saveFile(await residentReportsPdf(patientId, type))
    } catch (err) {
      setPdfError(err)
    } finally {
      setPdfBusy(false)
    }
  }

  const stays = data ? byStay(data.reports) : []
  const count = (type: ReportType) => (data ? data.reports.filter((r) => r.type === type).length : 0)

  return (
    <section className="panel" aria-labelledby="resident-reports-title">
      <div className="panel__header">
        <h2 id="resident-reports-title">{t('reports.resident.title')}</h2>
        <p className="panel__meta">{t('reports.resident.help')}</p>
      </div>
      <ErrorNotice error={error} onRetry={reload} />
      <ErrorNotice error={pdfError} title={t('reports.pdfFailed')} />
      {loading && !data ? (
        <LoadingRows rows={2} label={t('common.loading')} />
      ) : (
        data && (
          <>
            <ol className="resident-reports">
              {stays.map(({ start, end, reports }) => (
                <li key={start} className="resident-reports__stay">
                  <h3 className="resident-reports__title">
                    {t('reports.stay', {
                      from: formatDate(start, i18n.language),
                      to: end ? formatDate(end, i18n.language) : t('reports.stayOngoing'),
                    })}
                  </h3>
                  <ul className="resident-reports__list">
                    {reports.map((r) => (
                      <li key={r.id}>
                        <Link to={`/informes/${r.id}`}>{t(`reports.type.${r.type}`)}</Link>{' '}
                        <ReportStateTag state={r.state} />
                        <span className="muted">
                          {' '}
                          {t('reports.progress', { written: r.writtenSections, total: r.totalSections })}
                        </span>
                      </li>
                    ))}
                  </ul>
                </li>
              ))}
            </ol>
            <div className="resident-reports__actions">
              <Button kind="tertiary" size="sm" renderIcon={View} onClick={() => pdf(null, true)} disabled={pdfBusy}>
                {t('reports.resident.previewAll')}
              </Button>
              <Button kind="ghost" size="sm" renderIcon={Download} onClick={() => pdf(null, false)} disabled={pdfBusy}>
                {t('reports.resident.pdfAll')}
              </Button>
              {REPORT_TYPES.map((type) => (
                <Button key={type} kind="ghost" size="sm" renderIcon={Download} onClick={() => pdf(type, false)} disabled={pdfBusy}>
                  {t(`reports.resident.pdfType.${type}`, { count: count(type) })}
                </Button>
              ))}
            </div>
          </>
        )
      )}
    </section>
  )
}

interface Stay {
  start: string
  end: string | null
  reports: ReportSummary[]
}

// Agrupa por estancia, de la más reciente a la más antigua; dentro, media estancia antes que el final
function byStay(reports: ReportSummary[]): Stay[] {
  const stays = new Map<string, Stay>()
  for (const report of reports) {
    const stay = stays.get(report.stayStart) ?? { start: report.stayStart, end: null, reports: [] }
    stay.end = stay.end ?? report.stayEnd
    stay.reports.push(report)
    stays.set(report.stayStart, stay)
  }
  return [...stays.values()]
    .sort((a, b) => b.start.localeCompare(a.start))
    .map((stay) => ({ ...stay, reports: stay.reports.sort((a, b) => REPORT_TYPES.indexOf(a.type) - REPORT_TYPES.indexOf(b.type)) }))
}
