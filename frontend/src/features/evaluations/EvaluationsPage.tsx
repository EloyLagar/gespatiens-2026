import { Select, SelectItem, TextInput } from '@carbon/react'
import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { ACTIVITY_TYPES, getMonthlyEvaluations, recordMark, type ActivityType, type MonthlyEvaluations } from '../../api/diary'
import { beingEditedBy } from '../../api/editLocks'
import { ApiError } from '../../api/http'
import { useAuth } from '../../auth/AuthContext'
import { todayIso } from '../../shared/dates'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { formatScore, parseScore } from '../diary/activities'
import { average, columnsOf } from './grid'

// RF6 en tabla mensual (memoria final, 4.6.8): "se solicita mediante un formulario un mes y un año además de
// una materia". Una fila por residente, una columna por día, la media de cada semana y la del mes. Las notas
// son las mismas que se ponen en cada actividad del diario; aquí se ven de un vistazo y se pueden cambiar
export function EvaluationsPage() {
  const { t, i18n } = useTranslation()
  const { user } = useAuth()
  // Ponen notas los educadores y la administración (decisión del centro); los demás las consultan
  const canEvaluate = user?.speciality === 'SOCIAL_EDUCATOR' || user?.speciality === 'ADMINISTRATOR'
  const [month, setMonth] = useState(todayIso().slice(0, 7))
  const [type, setType] = useState<ActivityType>('LIFE_SKILLS')
  const { data, error, loading, reload } = useApi(() => getMonthlyEvaluations(month, type), [month, type])
  const [grid, setGrid] = useState<MonthlyEvaluations | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  useEffect(() => setGrid(data), [data])

  const locale = i18n.language === 'en' ? 'en-GB' : 'es-ES'
  const weekdayShort = new Intl.DateTimeFormat(locale, { weekday: 'narrow' })

  // La versión de cada actividad (RF8): cambia con cada nota guardada
  function versionOf(activityId: string): number {
    for (const day of grid?.days ?? []) {
      const found = day.activities.find((a) => a.id === activityId)
      if (found) return found.version
    }
    return 0
  }

  async function save(rowIndex: number, cellIndex: number, text: string) {
    if (!grid) return
    const row = grid.rows[rowIndex]
    const cell = row.cells[cellIndex]
    const mark = parseScore(text)
    if (mark === undefined) {
      setSaveError(t('evaluations.invalidMark'))
      return
    }
    if (!cell.activityId || mark === cell.mark) return
    setSaveError(null)
    const activityId = cell.activityId
    try {
      const { version } = await recordMark(activityId, row.resident.id, mark, versionOf(activityId))
      setGrid((current) => current && {
        ...current,
        days: current.days.map((day) => ({
          ...day,
          activities: day.activities.map((a) => (a.id === activityId ? { ...a, version } : a)),
        })),
        rows: current.rows.map((r, ri) => ri !== rowIndex ? r : {
          ...r,
          cells: r.cells.map((c, ci) => (ci === cellIndex ? { ...c, mark } : c)),
        }),
      })
    } catch (err) {
      const busy = beingEditedBy(err)
      setSaveError(busy
        ? t('evaluations.beingEdited', { name: busy.name ?? t('notices.formerEmployee') })
        : err instanceof ApiError && err.status === 409 ? t('evaluations.changed') : t('evaluations.notSaved'))
      // Lo que hay ahora, para no seguir sobre datos viejos
      reload()
    }
  }

  const columns = grid ? columnsOf(grid.days.map((d) => d.date)) : []

  return (
    <>
      <PageHeader title={t('evaluations.title')} subtitle={t('evaluations.subtitle')} />

      <div className="evaluations-filters">
        <TextInput
          id="evaluations-month"
          type="month"
          labelText={t('evaluations.month')}
          value={month}
          onChange={(e) => e.target.value && setMonth(e.target.value)}
        />
        <Select id="evaluations-type" labelText={t('evaluations.type')} value={type}
          onChange={(e) => setType(e.target.value as ActivityType)}>
          {ACTIVITY_TYPES.map((option) => (
            <SelectItem key={option} value={option} text={t(`diary.activities.type.${option}`)} />
          ))}
        </Select>
      </div>

      <p className="section-help">{canEvaluate ? t('evaluations.helpEdit') : t('evaluations.helpView')}</p>
      {saveError && <p className="form-error" role="alert">{saveError}</p>}

      {error ? (
        <ErrorNotice error={error} onRetry={reload} />
      ) : loading && !grid ? (
        <LoadingRows rows={6} label={t('common.loading')} />
      ) : grid && grid.days.every((d) => d.activities.length === 0) ? (
        <p className="empty">{t('evaluations.none', { type: t(`diary.activities.type.${type}`) })}</p>
      ) : grid && (
        <div className="table-wrap evaluations-grid">
          <table className="data-table">
            <caption className="visually-hidden">
              {t('evaluations.caption', { type: t(`diary.activities.type.${type}`), month })}
            </caption>
            <thead>
              <tr>
                <th scope="col" className="evaluations-grid__resident">{t('patient.fullName')}</th>
                {columns.map((column) =>
                  column.kind === 'day' ? (
                    <th key={column.date} scope="col"
                      className={grid.days[column.index].activities.length === 0 ? 'evaluations-grid__day is-empty' : 'evaluations-grid__day'}>
                      <span className="num">{Number(column.date.slice(8))}</span>
                      <span className="evaluations-grid__weekday">{weekdayShort.format(new Date(`${column.date}T12:00:00`))}</span>
                    </th>
                  ) : column.kind === 'week' ? (
                    <th key={`w${column.week}`} scope="col" className="evaluations-grid__average"
                      title={t('evaluations.weekAverage', { week: column.week })}>
                      {t('evaluations.weekShort', { week: column.week })}
                    </th>
                  ) : (
                    <th key="month" scope="col" className="evaluations-grid__average" title={t('evaluations.monthAverage')}>
                      {t('evaluations.monthShort')}
                    </th>
                  ),
                )}
              </tr>
            </thead>
            <tbody>
              {grid.rows.map((row, rowIndex) => {
                const name = `${row.resident.name} ${row.resident.surname}`
                return (
                  <tr key={row.resident.id}>
                    <th scope="row" className="evaluations-grid__resident">
                      {row.resident.number !== null && <span className="num">{row.resident.number} · </span>}
                      {name}
                    </th>
                    {columns.map((column) => {
                      if (column.kind === 'week') {
                        const value = average(row.cells.slice(column.from, column.to + 1))
                        return <td key={`w${column.week}`} className="num evaluations-grid__average">{formatScore(value, i18n.language)}</td>
                      }
                      if (column.kind === 'month') {
                        return <td key="month" className="num evaluations-grid__average">{formatScore(average(row.cells), i18n.language)}</td>
                      }
                      const cell = row.cells[column.index]
                      const day = Number(column.date.slice(8))
                      if (canEvaluate && cell.activityId) {
                        return (
                          <td key={column.date} className="evaluations-grid__cell">
                            <MarkInput
                              id={`mark-${row.resident.id}-${column.date}`}
                              label={t('evaluations.markOf', { name, day })}
                              value={formatScore(cell.mark, i18n.language)}
                              onCommit={(text) => save(rowIndex, column.index, text)}
                            />
                          </td>
                        )
                      }
                      return (
                        <td key={column.date} className={cell.attended ? 'num evaluations-grid__cell' : 'num evaluations-grid__cell is-absent'}>
                          {cell.mark !== null ? formatScore(cell.mark, i18n.language) : ''}
                        </td>
                      )
                    })}
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}

// Una nota editable: se guarda al salir de la casilla o con Intro, como en la tabla de la aplicación original
function MarkInput({ id, label, value, onCommit }: {
  id: string
  label: string
  value: string
  onCommit: (text: string) => void
}) {
  const [text, setText] = useState(value)
  // Escape deshace lo escrito: al perder el foco no se guarda
  const cancelled = useRef(false)
  useEffect(() => setText(value), [value])

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') event.currentTarget.blur()
    if (event.key === 'Escape') {
      cancelled.current = true
      setText(value)
      event.currentTarget.blur()
    }
  }

  function onBlur() {
    if (cancelled.current) {
      cancelled.current = false
      return
    }
    if (text !== value) onCommit(text)
  }

  return (
    <input
      id={id}
      className="mark-input num"
      inputMode="decimal"
      aria-label={label}
      value={text}
      onChange={(e) => setText(e.target.value)}
      onBlur={onBlur}
      onKeyDown={onKeyDown}
    />
  )
}
