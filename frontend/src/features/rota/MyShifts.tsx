import { Modal, Select, SelectItem } from '@carbon/react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { proposeSwap, type PlannedShift, type Rota, type ShiftSwap } from '../../api/rota'
import { formatDate, todayIso } from '../../shared/dates'
import { ErrorNotice } from '../../shared/ui'
import { byPart, indexShifts, shiftKey, weeksOf, monthName } from './calendar'
import { Legend } from './RotaGrid'

// "Mis turnos": el calendario del mes con los turnos de quien ha iniciado sesión. Desde cada turno que
// aún no ha llegado se puede pedir un cambio a un compañero
export function MyShifts({ rota, me, swaps, onProposed }: {
  rota: Rota
  me: string
  swaps: ShiftSwap[]
  onProposed: () => void
}) {
  const { t, i18n } = useTranslation()
  const [swapping, setSwapping] = useState<PlannedShift | null>(null)
  const today = todayIso()
  const mine = rota.shifts.filter((s) => s.employeeId === me)
  const locale = i18n.language === 'en' ? 'en-GB' : 'es-ES'
  const weekdays = Array.from({ length: 7 }, (_, i) =>
    new Intl.DateTimeFormat(locale, { weekday: 'short' }).format(new Date(2026, 9, 19 + i)))
  // Los turnos que ya tienen un cambio pedido y sin contestar
  const asked = new Set(swaps.filter((s) => s.status === 'PENDING' && s.requester?.id === me)
    .map((s) => s.requesterShift?.id))

  return (
    <section aria-labelledby="my-shifts-title">
      <h2 id="my-shifts-title" className="section-title">{t('rota.mine')}</h2>
      <p className="section-help">
        {mine.length === 0 ? t('rota.noShifts') : t('rota.myCount', { count: mine.length })}
      </p>
      <Legend />

      <table className="my-calendar">
        <caption className="visually-hidden">{t('rota.myCaption', { month: monthName(rota.month, i18n.language) })}</caption>
        <thead>
          <tr>
            {weekdays.map((day) => <th key={day} scope="col">{day}</th>)}
          </tr>
        </thead>
        <tbody>
          {weeksOf(rota.month).map((week, w) => (
            <tr key={w}>
              {week.map((date, d) => {
                if (!date) return <td key={d} className="my-calendar__blank" />
                const shifts = byPart(mine.filter((s) => s.date === date))
                const classes = ['my-calendar__day']
                if (date === today) classes.push('is-today')
                if (date < today) classes.push('is-past')
                return (
                  <td key={date} className={classes.join(' ')}>
                    <span className="my-calendar__number num">
                      {Number(date.slice(8))}
                      {date === today && <span className="visually-hidden"> ({t('rota.today')})</span>}
                    </span>
                    <ul className="my-calendar__shifts">
                      {shifts.map((shift) => (
                        <li key={shift.id}>
                          <span className={`rota-chip rota-chip--${shift.part} is-on`} aria-hidden="true">
                            {t(`rota.partShort.${shift.part}`)}
                          </span>
                          <span className="my-calendar__part">{t(`rota.part.${shift.part}`)}</span>
                          {asked.has(shift.id) ? (
                            <span className="my-calendar__asked">{t('rota.swapAsked')}</span>
                          ) : date >= today && (
                            <button type="button" className="text-action my-calendar__swap"
                              aria-label={t('rota.askSwapFor', { part: t(`rota.part.${shift.part}`), date: formatDate(date, i18n.language) })}
                              onClick={() => setSwapping(shift)}>
                              {t('rota.askSwap')}
                            </button>
                          )}
                        </li>
                      ))}
                    </ul>
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>

      {swapping && (
        <SwapDialog rota={rota} me={me} shift={swapping} onClose={() => setSwapping(null)}
          onDone={() => {
            setSwapping(null)
            onProposed()
          }} />
      )}
    </section>
  )
}

// Pedir un cambio: a quién y, si se quiere, qué turno suyo a cambio. Sin turno a cambio, el compañero
// solo cubre el mío. Se ofrecen los turnos del mes que se está viendo
function SwapDialog({ rota, me, shift, onClose, onDone }: {
  rota: Rota
  me: string
  shift: PlannedShift
  onClose: () => void
  onDone: () => void
}) {
  const { t, i18n } = useTranslation()
  const [partnerId, setPartnerId] = useState('')
  const [partnerShiftId, setPartnerShiftId] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const today = todayIso()
  const index = indexShifts(rota.shifts)
  const describe = (s: PlannedShift) => `${t(`rota.part.${s.part}`)} · ${formatDate(s.date, i18n.language)}`

  // Quien ya trabaja ese mismo turno no puede cubrirlo
  const colleagues = rota.employees.filter((e) => e.id !== me && !index.has(shiftKey(e.id, shift.date, shift.part)))
  // Sus turnos que aún no han llegado y que yo no tenga ya
  const theirShifts = byPart(rota.shifts.filter((s) => s.employeeId === partnerId && s.date >= today
    && !index.has(shiftKey(me, s.date, s.part))))
    .sort((a, b) => a.date.localeCompare(b.date))

  async function submit() {
    if (!partnerId) return
    setBusy(true)
    setError(null)
    try {
      await proposeSwap(shift.id, partnerId, partnerShiftId || null)
      onDone()
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      open
      modalHeading={t('rota.swapTitle')}
      primaryButtonText={busy ? t('common.working') : t('rota.sendSwap')}
      primaryButtonDisabled={busy || !partnerId}
      secondaryButtonText={t('common.cancel')}
      onRequestSubmit={submit}
      onRequestClose={onClose}
      preventCloseOnClickOutside
      size="sm"
    >
      <div className="dialog-body swap-dialog">
        <p>{t('rota.swapMine', { shift: describe(shift) })}</p>
        <Select id="swap-partner" labelText={t('rota.swapPartner')} value={partnerId}
          onChange={(e) => {
            setPartnerId(e.target.value)
            setPartnerShiftId('')
          }}>
          <SelectItem value="" text={t('rota.choose')} />
          {colleagues.map((e) => (
            <SelectItem key={e.id} value={e.id} text={`${e.name} · ${t(`speciality.${e.speciality}`)}`} />
          ))}
        </Select>
        <Select id="swap-their-shift" labelText={t('rota.swapTheirs')} value={partnerShiftId}
          disabled={!partnerId} helperText={t('rota.swapTheirsHelp')}
          onChange={(e) => setPartnerShiftId(e.target.value)}>
          <SelectItem value="" text={t('rota.justCover')} />
          {theirShifts.map((s) => <SelectItem key={s.id} value={s.id} text={describe(s)} />)}
        </Select>
        <ErrorNotice error={error} />
      </div>
    </Modal>
  )
}
