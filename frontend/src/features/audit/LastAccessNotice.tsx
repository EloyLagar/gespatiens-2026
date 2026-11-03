import { InlineNotification } from '@carbon/react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getMyLastAccess, type MyLastAccess } from '../../api/audit'
import { isLastAccessPending, setLastAccessPending } from '../../auth/session'
import { describeBrowser } from './AccessLogPage'

// ENS op.acc: nada más entrar, cada empleado ve cuándo y desde dónde fue su acceso anterior, y si desde entonces
// alguien ha fallado al entrar con su email. Es él quien sabe si fue él. Sale una vez por entrada, hasta que lo cierra
export function LastAccessNotice() {
  const { t, i18n } = useTranslation()
  const [data, setData] = useState<MyLastAccess | null>(null)

  useEffect(() => {
    if (!isLastAccessPending()) return
    let cancelled = false
    getMyLastAccess()
      .then((found) => {
        if (cancelled) return
        // Primera entrada y sin fallos: no hay nada que contarle
        if (!found.previous && found.failedAttemptsSince === 0) setLastAccessPending(false)
        else setData(found)
      })
      // Si no se puede saber, no se insiste: el aviso no es imprescindible para trabajar
      .catch(() => setLastAccessPending(false))
    return () => {
      cancelled = true
    }
  }, [])

  if (!data) return null

  const close = () => {
    setLastAccessPending(false)
    setData(null)
  }
  const when = new Intl.DateTimeFormat(i18n.language === 'en' ? 'en-GB' : 'es-ES', { dateStyle: 'full', timeStyle: 'short' })
  const p = data.previous
  const where = p ? [p.ipAddress, describeBrowser(p.userAgent)].filter(Boolean).join(' · ') : ''
  const failed = data.failedAttemptsSince

  return (
    <div className="last-access">
      <InlineNotification
        kind={failed > 0 ? 'warning' : 'info'}
        lowContrast
        role="status"
        title={p ? t('lastAccess.previous', { when: when.format(new Date(p.occurredAt)) }) : t('lastAccess.first')}
        subtitle={[
          where && t('lastAccess.from', { where }),
          failed > 0 && t('lastAccess.failed', { count: failed }),
        ].filter(Boolean).join(' ')}
        statusIconDescription={t(failed > 0 ? 'lastAccess.warningIcon' : 'lastAccess.infoIcon')}
        aria-label={t('lastAccess.close')}
        onClose={() => {
          close()
          return true
        }}
      />
    </div>
  )
}
