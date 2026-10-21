import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { assignShift, SHIFT_PARTS, unassignShift, type PlannedShift, type Rota, type ShiftPart } from '../../api/rota'
import type { IsoDate } from '../../api/types'
import { todayIso } from '../../shared/dates'
import { useErrorMessage } from '../../shared/ui'
import { coverage, daysOf, indexShifts, shiftKey, weekdayOf, monthName } from './calendar'

// El cuadrante de todos: una fila por trabajador y una columna por día, con mañana, tarde y noche en cada
// casilla. La administración lo planifica pulsando las casillas; el resto de la plantilla lo consulta
export function RotaGrid({ rota, me, editable, onChanged }: {
  rota: Rota
  me: string
  editable: boolean
  onChanged: () => void
}) {
  const { t, i18n } = useTranslation()
  const message = useErrorMessage()
  const [shifts, setShifts] = useState<PlannedShift[]>(rota.shifts)
  const [busy, setBusy] = useState<Set<string>>(new Set())
  const [error, setError] = useState<string | null>(null)
  useEffect(() => setShifts(rota.shifts), [rota])

  const days = daysOf(rota.month)
  const index = useMemo(() => indexShifts(shifts), [shifts])
  const today = todayIso()
  const locale = i18n.language === 'en' ? 'en-GB' : 'es-ES'
  const weekdayShort = new Intl.DateTimeFormat(locale, { weekday: 'narrow' })

  async function toggle(employeeId: string, date: IsoDate, part: ShiftPart) {
    const key = shiftKey(employeeId, date, part)
    if (busy.has(key)) return
    setBusy((current) => new Set(current).add(key))
    setError(null)
    try {
      const existing = index.get(key)
      if (existing) {
        await unassignShift(existing.id)
        setShifts((current) => current.filter((s) => s.id !== existing.id))
      } else {
        const created = await assignShift(date, part, employeeId)
        setShifts((current) => [...current, created])
      }
    } catch (err) {
      setError(message(err))
      // Lo que hay ahora: puede que otra persona lo haya cambiado
      onChanged()
    } finally {
      setBusy((current) => {
        const next = new Set(current)
        next.delete(key)
        return next
      })
    }
  }

  if (rota.employees.length === 0) return <p className="empty">{t('rota.noStaff')}</p>

  return (
    <section aria-labelledby="rota-grid-title">
      <h2 id="rota-grid-title" className="section-title">{t('rota.everyone')}</h2>
      <p className="section-help">{editable ? t('rota.helpEdit') : t('rota.helpView')}</p>
      <Legend />
      {error && <p className="form-error" role="alert">{error}</p>}

      <div className="table-wrap rota-grid">
        <table className="data-table">
          <caption className="visually-hidden">{t('rota.caption', { month: monthName(rota.month, i18n.language) })}</caption>
          <thead>
            <tr>
              <th scope="col" className="rota-grid__employee">{t('rota.employee')}</th>
              {days.map((date) => (
                <th key={date} scope="col" className={dayClass(date, today)}>
                  <span className="num">{Number(date.slice(8))}</span>
                  <span className="rota-grid__weekday">{weekdayShort.format(new Date(`${date}T12:00:00`))}</span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rota.employees.map((employee) => (
              <tr key={employee.id} className={employee.id === me ? 'is-me' : undefined}>
                <th scope="row" className="rota-grid__employee">
                  <span className="rota-grid__name">{employee.name}</span>
                  <span className="rota-grid__speciality">{t(`speciality.${employee.speciality}`)}</span>
                </th>
                {days.map((date) => (
                  <td key={date} className={dayClass(date, today)}>
                    <div className="rota-cell">
                      {SHIFT_PARTS.map((part) => {
                        const key = shiftKey(employee.id, date, part)
                        const on = index.has(key)
                        const label = t('rota.cellLabel', {
                          part: t(`rota.part.${part}`), name: employee.name, day: Number(date.slice(8)),
                        })
                        return editable ? (
                          <button key={part} type="button" className={`rota-chip rota-chip--${part}`}
                            aria-pressed={on} aria-label={label} title={label} disabled={busy.has(key)}
                            onClick={() => toggle(employee.id, date, part)}>
                            {t(`rota.partShort.${part}`)}
                          </button>
                        ) : on ? (
                          <span key={part} className={`rota-chip rota-chip--${part} is-on`} title={label}>
                            <span aria-hidden="true">{t(`rota.partShort.${part}`)}</span>
                            <span className="visually-hidden">{t(`rota.part.${part}`)}</span>
                          </span>
                        ) : null
                      })}
                    </div>
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
          {/* Cuántas personas hay cada día en cada parte: lo que mira quien planifica */}
          <tfoot>
            {SHIFT_PARTS.map((part) => (
              <tr key={part}>
                <th scope="row" className="rota-grid__employee rota-grid__coverage">
                  {t('rota.coverage', { part: t(`rota.part.${part}`) })}
                </th>
                {days.map((date) => {
                  const count = coverage(shifts, date, part)
                  return (
                    <td key={date} className={`num ${dayClass(date, today)}${count === 0 ? ' is-uncovered' : ''}`}>
                      {count}
                    </td>
                  )
                })}
              </tr>
            ))}
          </tfoot>
        </table>
      </div>
    </section>
  )
}

function dayClass(date: IsoDate, today: IsoDate): string {
  const classes = ['rota-grid__day']
  if (weekdayOf(date) >= 5) classes.push('is-weekend')
  if (date === today) classes.push('is-today')
  return classes.join(' ')
}

// Las letras se explican siempre: el color solo acompaña
export function Legend() {
  const { t } = useTranslation()
  return (
    <ul className="rota-legend" aria-label={t('rota.legend')}>
      {SHIFT_PARTS.map((part) => (
        <li key={part}>
          <span className={`rota-chip rota-chip--${part} is-on`} aria-hidden="true">{t(`rota.partShort.${part}`)}</span>
          {t(`rota.part.${part}`)}
        </li>
      ))}
    </ul>
  )
}
