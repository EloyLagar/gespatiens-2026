import { InlineNotification, Modal } from '@carbon/react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../api/http'
import { LANGUAGES } from '../i18n'

// El logotipo (RNF5), el de la aplicación original: "ges" en aguamarina y "patiens" en el azul de la
// aplicación, con la tipografía Outfit (memoria final, 4.4). Es texto, así que el lector de pantalla lo lee
export function Wordmark({ size = 24 }: { size?: number }) {
  return (
    <span className="wordmark" style={{ fontSize: size }}>
      <span className="wordmark__ges">ges</span>
      <span className="wordmark__patiens">patiens</span>
    </span>
  )
}

interface PageHeaderProps {
  title: string
  subtitle?: ReactNode
  actions?: ReactNode
  breadcrumb?: ReactNode
}

export function PageHeader({ title, subtitle, actions, breadcrumb }: PageHeaderProps) {
  return (
    <header className="page-header">
      {breadcrumb && <nav className="page-header__crumbs">{breadcrumb}</nav>}
      <div className="page-header__row">
        <div className="page-header__text">
          <h1>{title}</h1>
          {/* div y no p: el subtítulo a veces lleva una etiqueta de Carbon (Tag), que es un div */}
          {subtitle && <div className="page-header__subtitle">{subtitle}</div>}
        </div>
        {actions && <div className="page-header__actions">{actions}</div>}
      </div>
    </header>
  )
}

// Traduce un error de la API a un mensaje. El detalle de Spring (ProblemDetail.detail) viene en
// castellano; en inglés se muestra un mensaje genérico por código de estado (ver DESIGN.md, RNF9)
export function useErrorMessage() {
  const { t, i18n } = useTranslation()
  return (error: unknown): string => {
    if (error instanceof ApiError) {
      if (error.status === 0) return t('errors.network')
      if (error.detail && i18n.language === 'es') return error.detail
      const byStatus: Record<number, string> = {
        400: t('errors.badRequest'),
        401: t('errors.unauthorized'),
        403: t('errors.forbidden'),
        404: t('errors.notFound'),
        409: t('errors.conflict'),
        410: t('errors.gone'),
        429: t('errors.tooManyAttempts'),
        502: t('errors.badGateway'),
      }
      return byStatus[error.status] ?? t('errors.generic')
    }
    return t('errors.generic')
  }
}

export function ErrorNotice({ error, title, onRetry }: { error: unknown; title?: string; onRetry?: () => void }) {
  const { t } = useTranslation()
  const message = useErrorMessage()
  if (!error) return null
  return (
    <div role="alert" className="error-notice">
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={title ?? t('errors.title')}
        subtitle={message(error)}
      />
      {onRetry && (
        <button type="button" className="text-action" onClick={onRetry}>
          {t('common.retry')}
        </button>
      )}
    </div>
  )
}

export function SuccessNotice({ message }: { message: string | null }) {
  // role="status": el lector de pantalla lo anuncia sin interrumpir
  return (
    <div role="status" aria-live="polite" className="success-notice">
      {message && <InlineNotification kind="success" lowContrast hideCloseButton title={message} />}
    </div>
  )
}

interface ConfirmDialogProps {
  open: boolean
  title: string
  confirmLabel: string
  danger?: boolean
  busy?: boolean
  error?: unknown
  onConfirm: () => void
  onClose: () => void
  children: ReactNode
}

// Toda acción destructiva o de estado (baja, reingreso, borrado) pasa por aquí y nombra a la persona
export function ConfirmDialog({ open, title, confirmLabel, danger, busy, error, onConfirm, onClose, children }: ConfirmDialogProps) {
  const { t } = useTranslation()
  return (
    <Modal
      open={open}
      danger={danger}
      modalHeading={title}
      primaryButtonText={busy ? t('common.working') : confirmLabel}
      primaryButtonDisabled={busy}
      secondaryButtonText={t('common.cancel')}
      onRequestSubmit={onConfirm}
      onRequestClose={onClose}
      preventCloseOnClickOutside
      size="sm"
    >
      <div className="dialog-body">{children}</div>
      <ErrorNotice error={error} />
    </Modal>
  )
}

// Carga: la forma final con barras grises planas a su tamaño real, sin brillo animado
export function LoadingRows({ rows = 5, label }: { rows?: number; label: string }) {
  return (
    <div className="loading-rows" role="status" aria-label={label}>
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="loading-rows__row">
          <span className="bar bar--short" />
          <span className="bar bar--long" />
          <span className="bar bar--mid" />
        </div>
      ))}
    </div>
  )
}

// onChange: además de cambiarlo en pantalla, guardarlo (en el perfil del empleado que ha iniciado sesión)
export function LanguageSwitch({ onChange }: { onChange?: (language: (typeof LANGUAGES)[number]) => void }) {
  const { t, i18n } = useTranslation()
  return (
    <div className="language-switch" role="group" aria-label={t('language.label')}>
      {LANGUAGES.map((language) => (
        <button
          key={language}
          type="button"
          lang={language}
          aria-pressed={i18n.language === language}
          onClick={() => {
            i18n.changeLanguage(language)
            onChange?.(language)
          }}
        >
          {t(`language.${language}`)}
        </button>
      ))}
    </div>
  )
}

// Rótulo de dato: etiqueta gris encima, valor en tinta. Un valor vacío es una raya gris
// span: el dato ocupa dos columnas ('two') o toda la fila ('all'). Va en el propio div del par dt/dd: un <dl>
// solo admite un nivel de div entre él y sus dt/dd (WCAG, regla definition-list)
export function Field({ label, children, numeric, span }: { label: string; children: ReactNode; numeric?: boolean; span?: 'two' | 'all' }) {
  const empty = children === null || children === undefined || children === ''
  const spanClass = span === 'all' ? ' kv-span kv-span--all' : span === 'two' ? ' kv-span' : ''
  return (
    <div className={`kv${spanClass}`}>
      <dt>{label}</dt>
      <dd className={numeric ? 'num' : undefined}>{empty ? <span className="dash">—</span> : children}</dd>
    </div>
  )
}
