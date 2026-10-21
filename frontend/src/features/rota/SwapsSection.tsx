import { Button, Tag } from '@carbon/react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { resolveSwap, type PlannedShift, type ShiftSwap } from '../../api/rota'
import { formatDate } from '../../shared/dates'
import { ErrorNotice, LoadingRows, SuccessNotice, useErrorMessage } from '../../shared/ui'

const TAG: Record<ShiftSwap['status'], 'blue' | 'green' | 'gray' | 'red'> = {
  PENDING: 'blue',
  ACCEPTED: 'green',
  DECLINED: 'red',
  CANCELLED: 'gray',
}

// Los cambios de turno de quien ha iniciado sesión: los que le piden (aceptar o rechazar), los que ha pedido
// (retirarlos) y los ya resueltos. Se acuerdan entre compañeros; la administración no tiene que aprobarlos
export function SwapsSection({ swaps, error, me, onRetry, onChanged }: {
  swaps: ShiftSwap[] | null
  error: unknown
  me: string
  onRetry: () => void
  onChanged: () => void
}) {
  const { t, i18n } = useTranslation()
  const message = useErrorMessage()
  const [busy, setBusy] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [done, setDone] = useState<string | null>(null)

  const describe = (s: PlannedShift | null) =>
    s ? `${t(`rota.part.${s.part}`).toLowerCase()} ${t('rota.ofDay')} ${formatDate(s.date, i18n.language)}` : t('rota.goneShift')
  const nameOf = (e: ShiftSwap['requester']) => e?.name ?? t('notices.formerEmployee')

  async function act(swap: ShiftSwap, action: 'accept' | 'decline' | 'cancel') {
    setBusy(swap.id)
    setActionError(null)
    setDone(null)
    try {
      await resolveSwap(swap.id, action)
      setDone(t(`rota.done.${action}`))
    } catch (err) {
      setActionError(message(err))
    } finally {
      setBusy(null)
      onChanged()
    }
  }

  // Qué se pide, contado desde quien mira
  function sentence(swap: ShiftSwap): string {
    const incoming = swap.partner?.id === me
    const values = {
      name: nameOf(incoming ? swap.requester : swap.partner),
      mine: describe(swap.requesterShift),
      theirs: describe(swap.partnerShift),
    }
    if (incoming) return swap.partnerShift ? t('rota.incomingSwap', values) : t('rota.incomingCover', values)
    return swap.partnerShift ? t('rota.outgoingSwap', values) : t('rota.outgoingCover', values)
  }

  if (error) return <ErrorNotice error={error} onRetry={onRetry} />
  if (!swaps) return <LoadingRows rows={2} label={t('common.loading')} />

  const pending = swaps.filter((s) => s.status === 'PENDING')
  const resolved = swaps.filter((s) => s.status !== 'PENDING').slice(0, 10)

  return (
    <section aria-labelledby="swaps-title" className="swaps">
      <h2 id="swaps-title" className="section-title">{t('rota.swaps')}</h2>
      <p className="section-help">{t('rota.swapsHelp')}</p>
      <SuccessNotice message={done} />
      {actionError && <p className="form-error" role="alert">{actionError}</p>}

      {pending.length === 0 ? (
        <p className="empty">{t('rota.noPending')}</p>
      ) : (
        <ul className="swap-list">
          {pending.map((swap) => (
            <li key={swap.id} className="swap-list__item">
              <span>{sentence(swap)}</span>
              <span className="swap-list__actions">
                {swap.partner?.id === me ? (
                  <>
                    <Button size="sm" disabled={busy === swap.id} onClick={() => act(swap, 'accept')}>
                      {t('rota.accept')}
                    </Button>
                    <Button size="sm" kind="tertiary" disabled={busy === swap.id} onClick={() => act(swap, 'decline')}>
                      {t('rota.decline')}
                    </Button>
                  </>
                ) : (
                  <Button size="sm" kind="ghost" disabled={busy === swap.id} onClick={() => act(swap, 'cancel')}>
                    {t('rota.cancelSwap')}
                  </Button>
                )}
              </span>
            </li>
          ))}
        </ul>
      )}

      {resolved.length > 0 && (
        <>
          <h3 className="swaps__history">{t('rota.history')}</h3>
          <ul className="swap-list">
            {resolved.map((swap) => (
              <li key={swap.id} className="swap-list__item is-resolved">
                <span>{sentence(swap)}</span>
                <Tag type={TAG[swap.status]} size="sm">{t(`rota.status.${swap.status}`)}</Tag>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
