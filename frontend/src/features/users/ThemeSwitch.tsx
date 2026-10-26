import { Moon, Screen, Sun } from '@carbon/icons-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { changeMyTheme } from '../../api/users'
import { THEMES, type Theme } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { applyTheme, currentTheme } from '../../theme'

const ICONS: Record<Theme, typeof Sun> = { SYSTEM: Screen, LIGHT: Sun, DARK: Moon }

// Claro, oscuro o el del equipo: una fila más del pie del menú, como "Cerrar sesión". Dice el tema actual y al
// pulsarla pasa al siguiente (sistema, claro, oscuro). Cambia al momento y se guarda en el perfil, como el
// idioma: si no se puede guardar (sin red), la pantalla ya ha cambiado y solo se pierde que lo recuerde en
// otro equipo
export function ThemeSwitch({ rail }: { rail?: boolean }) {
  const { t } = useTranslation()
  const { user, replaceUser } = useAuth()
  const [theme, setTheme] = useState<Theme>(user?.theme ?? currentTheme())
  const next = THEMES[(THEMES.indexOf(theme) + 1) % THEMES.length]
  const Icon = ICONS[theme]
  const label = t('theme.current', { theme: t(`theme.${theme}`) })

  function choose() {
    setTheme(next)
    applyTheme(next)
    if (user) replaceUser({ ...user, theme: next })
    changeMyTheme(next).catch(() => {})
  }

  return (
    <button
      type="button"
      className="side-nav__link side-nav__theme"
      onClick={choose}
      // Plegado solo se ve el icono: el nombre va en el title, como en el resto del menú
      title={rail ? label : t('theme.switchTo', { theme: t(`theme.${next}`) })}
    >
      <Icon size={16} aria-hidden="true" />
      <span>{label}</span>
    </button>
  )
}
