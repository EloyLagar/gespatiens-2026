import { ChevronLeft, ChevronRight } from '@carbon/icons-react'
import { IconButton } from '@carbon/react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getRota, mySwaps } from '../../api/rota'
import { useAuth } from '../../auth/AuthContext'
import { todayIso } from '../../shared/dates'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { addMonths, monthName } from './calendar'
import { MyShifts } from './MyShifts'
import { PlanningTools } from './PlanningTools'
import { RotaGrid } from './RotaGrid'
import { SwapsSection } from './SwapsSection'

type View = 'mine' | 'rota'

// El cuadrante de turnos: planificación, no un sistema de fichajes. Cada trabajador ve sus turnos y acuerda
// cambios con sus compañeros; el cuadrante de todos lo ve toda la plantilla y lo planifica la administración
export function RotaPage() {
  const { t, i18n } = useTranslation()
  const { user, isAdmin } = useAuth()
  const [view, setView] = useState<View>(isAdmin ? 'rota' : 'mine')
  const [month, setMonth] = useState(todayIso().slice(0, 7))
  const rota = useApi(() => getRota(month), [month])
  const swaps = useApi(() => mySwaps(), [])

  if (!user) return null

  // Un cambio aceptado mueve turnos: se recargan las dos cosas
  const reloadAll = () => {
    rota.reload()
    swaps.reload()
  }

  const monthLabel = monthName(month, i18n.language, true)

  return (
    <>
      <PageHeader title={t('rota.title')} subtitle={t('rota.subtitle')} />

      <div className="rota-toolbar">
        <div className="segmented" role="group" aria-label={t('rota.view')}>
          <button type="button" aria-pressed={view === 'mine'} onClick={() => setView('mine')}>
            {t('rota.mine')}
          </button>
          <button type="button" aria-pressed={view === 'rota'} onClick={() => setView('rota')}>
            {t('rota.everyone')}
          </button>
        </div>
        <div className="rota-month">
          <IconButton kind="ghost" size="sm" label={t('rota.previousMonth')} onClick={() => setMonth(addMonths(month, -1))}>
            <ChevronLeft />
          </IconButton>
          <h2 className="rota-month__label" aria-live="polite">{monthLabel}</h2>
          <IconButton kind="ghost" size="sm" label={t('rota.nextMonth')} onClick={() => setMonth(addMonths(month, 1))}>
            <ChevronRight />
          </IconButton>
          {month !== todayIso().slice(0, 7) && (
            <button type="button" className="text-action" onClick={() => setMonth(todayIso().slice(0, 7))}>
              {t('rota.thisMonth')}
            </button>
          )}
        </div>
      </div>

      {rota.error ? (
        <ErrorNotice error={rota.error} onRetry={rota.reload} />
      ) : !rota.data ? (
        <LoadingRows rows={6} label={t('common.loading')} />
      ) : view === 'mine' ? (
        <>
          <MyShifts rota={rota.data} me={user.id} swaps={swaps.data ?? []} onProposed={swaps.reload} />
          <SwapsSection swaps={swaps.data} error={swaps.error} me={user.id} onRetry={swaps.reload} onChanged={reloadAll} />
        </>
      ) : (
        <>
          {isAdmin && <PlanningTools rota={rota.data} onChanged={rota.reload} />}
          <RotaGrid rota={rota.data} me={user.id} editable={isAdmin} onChanged={rota.reload} />
        </>
      )}
    </>
  )
}
