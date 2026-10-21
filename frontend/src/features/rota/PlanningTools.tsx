import { Button, Checkbox, Select, SelectItem, TextInput } from '@carbon/react'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import {
  clearRange, copyMonth, fillRange, repeatWeek, SHIFT_PARTS, WEEKDAYS, type Rota, type ShiftPart, type Weekday,
} from '../../api/rota'
import { ConfirmDialog, SuccessNotice, useErrorMessage } from '../../shared/ui'
import { addMonths, daysOf, mondayOf, monthName } from './calendar'

// Las herramientas de la administración para no tener que pulsar casilla a casilla: rellenar un periodo,
// vaciarlo, repetir una semana y copiar el mes anterior. Ninguna duplica turnos que ya estén puestos
export function PlanningTools({ rota, onChanged }: { rota: Rota; onChanged: () => void }) {
  const { t, i18n } = useTranslation()
  const message = useErrorMessage()
  const days = daysOf(rota.month)
  const first = days[0]
  const last = days[days.length - 1]

  const [employeeId, setEmployeeId] = useState('')
  const [from, setFrom] = useState(first)
  const [to, setTo] = useState(last)
  const [weekdays, setWeekdays] = useState<Weekday[]>(WEEKDAYS.slice(0, 5))
  const [parts, setParts] = useState<ShiftPart[]>(['MORNING'])
  const [sourceMonday, setSourceMonday] = useState(mondayOf(first))
  const [until, setUntil] = useState(last)
  const [confirmCopy, setConfirmCopy] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState<string | null>(null)

  // Al cambiar de mes, las fechas se ponen en el mes que se está viendo
  const [month, setMonth] = useState(rota.month)
  if (month !== rota.month) {
    setMonth(rota.month)
    setFrom(first)
    setTo(last)
    setSourceMonday(mondayOf(first))
    setUntil(last)
  }

  async function run(action: () => Promise<{ changed: number }>, key: 'added' | 'removed') {
    setBusy(true)
    setError(null)
    setDone(null)
    try {
      const { changed } = await action()
      setDone(t(`rota.tools.${key}`, { count: changed }))
      onChanged()
    } catch (err) {
      setError(message(err))
    } finally {
      setBusy(false)
    }
  }

  function toggle<T>(list: T[], value: T): T[] {
    return list.includes(value) ? list.filter((v) => v !== value) : [...list, value]
  }

  function fill(event: FormEvent) {
    event.preventDefault()
    if (!employeeId || weekdays.length === 0 || parts.length === 0) {
      setError(t('rota.tools.fillMissing'))
      return
    }
    run(() => fillRange({ employeeId, from, to, weekdays, parts }), 'added')
  }

  function clear() {
    if (!employeeId) {
      setError(t('rota.tools.chooseEmployee'))
      return
    }
    run(() => clearRange({ employeeId, from, to }), 'removed')
  }

  const previous = addMonths(rota.month, -1)

  return (
    <details className="planning-tools">
      <summary>{t('rota.tools.title')}</summary>
      <SuccessNotice message={done} />
      {error && <p className="form-error" role="alert">{error}</p>}

      <form className="planning-tools__block" onSubmit={fill}>
        <h3>{t('rota.tools.fillTitle')}</h3>
        <p className="section-help">{t('rota.tools.fillHelp')}</p>
        <div className="planning-tools__row">
          <Select id="tools-employee" labelText={t('rota.employee')} value={employeeId}
            onChange={(e) => setEmployeeId(e.target.value)}>
            <SelectItem value="" text={t('rota.choose')} />
            {rota.employees.map((e) => <SelectItem key={e.id} value={e.id} text={e.name} />)}
          </Select>
          <TextInput id="tools-from" type="date" labelText={t('rota.tools.from')} value={from}
            onChange={(e) => setFrom(e.target.value)} />
          <TextInput id="tools-to" type="date" labelText={t('rota.tools.to')} value={to}
            onChange={(e) => setTo(e.target.value)} />
        </div>
        <fieldset className="planning-tools__checks">
          <legend>{t('rota.tools.weekdays')}</legend>
          {WEEKDAYS.map((day) => (
            <Checkbox key={day} id={`tools-day-${day}`} labelText={t(`rota.weekday.${day}`)}
              checked={weekdays.includes(day)} onChange={() => setWeekdays(toggle(weekdays, day))} />
          ))}
        </fieldset>
        <fieldset className="planning-tools__checks">
          <legend>{t('rota.tools.parts')}</legend>
          {SHIFT_PARTS.map((part) => (
            <Checkbox key={part} id={`tools-part-${part}`} labelText={t(`rota.part.${part}`)}
              checked={parts.includes(part)} onChange={() => setParts(toggle(parts, part))} />
          ))}
        </fieldset>
        <div className="planning-tools__buttons">
          <Button type="submit" size="md" disabled={busy}>{t('rota.tools.fill')}</Button>
          <Button type="button" size="md" kind="tertiary" disabled={busy} onClick={clear}>{t('rota.tools.clear')}</Button>
        </div>
      </form>

      <div className="planning-tools__block">
        <h3>{t('rota.tools.repeatTitle')}</h3>
        <p className="section-help">{t('rota.tools.repeatHelp')}</p>
        <div className="planning-tools__row">
          <TextInput id="tools-week" type="date" labelText={t('rota.tools.week')} value={sourceMonday}
            helperText={t('rota.tools.weekHelp')}
            onChange={(e) => e.target.value && setSourceMonday(mondayOf(e.target.value))} />
          <TextInput id="tools-until" type="date" labelText={t('rota.tools.until')} value={until}
            onChange={(e) => setUntil(e.target.value)} />
        </div>
        <div className="planning-tools__buttons">
          <Button size="md" kind="tertiary" disabled={busy}
            onClick={() => run(() => repeatWeek(sourceMonday, until), 'added')}>
            {t('rota.tools.repeat')}
          </Button>
        </div>
      </div>

      <div className="planning-tools__block">
        <h3>{t('rota.tools.copyTitle')}</h3>
        <p className="section-help">{t('rota.tools.copyHelp')}</p>
        <div className="planning-tools__buttons">
          <Button size="md" kind="tertiary" disabled={busy} onClick={() => setConfirmCopy(true)}>
            {t('rota.tools.copy', { from: monthName(previous, i18n.language) })}
          </Button>
        </div>
      </div>

      <ConfirmDialog
        open={confirmCopy}
        title={t('rota.tools.copyTitle')}
        confirmLabel={t('rota.tools.copyConfirm')}
        busy={busy}
        onClose={() => setConfirmCopy(false)}
        onConfirm={() => {
          setConfirmCopy(false)
          run(() => copyMonth(previous, rota.month), 'added')
        }}
      >
        <p>{t('rota.tools.copyQuestion', { from: monthName(previous, i18n.language), to: monthName(rota.month, i18n.language) })}</p>
      </ConfirmDialog>
    </details>
  )
}
