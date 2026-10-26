import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { LanguageSwitch, Wordmark } from '../../shared/ui'

const ISSUES = 'https://github.com/EloyLagar/gespatiens-2026/issues'

// La declaración de accesibilidad: qué cumple la aplicación, cómo se comprueba y lo que todavía no. Se abre sin
// haber entrado (enlace en el login), como las de las webs públicas
export function AccessibilityPage() {
  const { t } = useTranslation()
  const { user } = useAuth()

  function list(key: string) {
    return (
      <ul className="statement__list">
        {(t(key, { returnObjects: true }) as string[]).map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    )
  }

  return (
    <main className="login statement">
      <div className="login__panel">
        <div className="login__brand">
          <div>
            <p className="login__name"><Wordmark size={32} /></p>
            <p className="login__scope">{t('app.scope')}</p>
          </div>
        </div>

        <h1>{t('accessibility.title')}</h1>
        <p className="login__lead">{t('accessibility.lead')}</p>

        <section aria-labelledby="statement-status">
          <h2 id="statement-status">{t('accessibility.statusTitle')}</h2>
          <p>{t('accessibility.status')}</p>
        </section>

        <section aria-labelledby="statement-features">
          <h2 id="statement-features">{t('accessibility.featuresTitle')}</h2>
          {list('accessibility.features')}
        </section>

        <section aria-labelledby="statement-method">
          <h2 id="statement-method">{t('accessibility.methodTitle')}</h2>
          {list('accessibility.method')}
        </section>

        <section aria-labelledby="statement-limits">
          <h2 id="statement-limits">{t('accessibility.limitsTitle')}</h2>
          {list('accessibility.limits')}
        </section>

        <section aria-labelledby="statement-contact">
          <h2 id="statement-contact">{t('accessibility.contactTitle')}</h2>
          <p>
            {t('accessibility.contact')} <a href={ISSUES}>{t('accessibility.contactLink')}</a>.
          </p>
        </section>

        <p className="login__help">{t('accessibility.updated')}</p>
        <p>
          <Link to={user ? '/avisos' : '/login'}>{t('accessibility.back')}</Link>
        </p>
        <LanguageSwitch />
      </div>
    </main>
  )
}
