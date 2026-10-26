import { Moon, Screen, Sun } from '@carbon/icons-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { changeMyTheme } from '../../api/users'
import { THEMES, type Theme } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { applyTheme, currentTheme } from '../../theme'

const ICONS: Record<Theme, typeof Sun> = { SYSTEM: Screen, LIGHT: Sun, DARK: Moon }

// Claro, oscuro o el del equipo, en el menú junto al idioma. Cambia al momento y se guarda en el perfil, como
// el idioma: si no se puede guardar (sin red), la pantalla ya ha cambiado y solo se pierde que lo recuerde en
// otro equipo. Son iconos para que quepa al lado de ES/EN: el nombre va en aria-label y en el title
export function ThemeSwitch() {
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
    <div className="language-switch theme-switch" role="group" aria-label={t('theme.label')}>
      {THEMES.map((option) => {
        const Icon = ICONS[option]
        return (
          <button
            key={option}
            type="button"
            aria-pressed={theme === option}
            aria-label={t(`theme.${option}`)}
            title={t(`theme.${option}`)}
            onClick={() => choose(option)}
          >
            <Icon size={16} aria-hidden="true" />
          </button>
        )
      })}
    </div>
  )
}
