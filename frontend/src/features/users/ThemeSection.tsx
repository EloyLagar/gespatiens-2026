import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { changeMyTheme } from '../../api/users'
import { THEMES, type Theme } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { applyTheme, currentTheme } from '../../theme'

// Claro, oscuro o el del equipo. Cambia al momento y se guarda en el perfil, como el idioma: si no se puede
// guardar (sin red), la pantalla ya ha cambiado y solo se pierde que lo recuerde en otro equipo
export function ThemeSection() {
  const { t } = useTranslation()
  const { user, replaceUser } = useAuth()
  const [theme, setTheme] = useState<Theme>(user?.theme ?? currentTheme())

  function choose(next: Theme) {
    setTheme(next)
    applyTheme(next)
    if (user) replaceUser({ ...user, theme: next })
    changeMyTheme(next).catch(() => {})
  }

  return (
    <section className="settings-row" aria-labelledby="my-theme-title">
      <div className="settings-row__label">
        <h2 id="my-theme-title">{t('profile.theme.title')}</h2>
        <p>{t('profile.theme.help')}</p>
      </div>
      <div className="settings-row__panel">
        <div className="segmented" role="group" aria-labelledby="my-theme-title">
          {THEMES.map((option) => (
            <button key={option} type="button" aria-pressed={theme === option} onClick={() => choose(option)}>
              {t(`profile.theme.${option}`)}
            </button>
          ))}
        </div>
      </div>
    </section>
  )
}
