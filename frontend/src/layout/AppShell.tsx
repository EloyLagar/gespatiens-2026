import { Archive, Close, Group, Logout, Menu, Notebook, Notification, UserMultiple, UserProfile } from '@carbon/icons-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { LanguageSwitch, Mark } from '../shared/ui'

interface NavItem {
  to: string
  label: string
  icon: ReactNode
}

export function AppShell() {
  const { t } = useTranslation()
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const menuButton = useRef<HTMLButtonElement>(null)

  // Al navegar desde el menú del móvil, se cierra
  useEffect(() => setMenuOpen(false), [location.pathname])

  // Escape cierra el menú y devuelve el foco al botón que lo abrió
  useEffect(() => {
    if (!menuOpen) return
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMenuOpen(false)
        menuButton.current?.focus()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [menuOpen])

  if (!user) return null

  // RNF3: cada empleado ve los apartados de la aplicación. Las acciones que no puede hacer
  // (dar de baja, borrar, gestionar empleados) se ocultan dentro de cada pantalla
  const items: NavItem[] = [
    { to: '/avisos', label: t('nav.notices'), icon: <Notification size={16} /> },
    { to: '/diario', label: t('nav.diary'), icon: <Notebook size={16} /> },
    { to: '/residentes', label: t('nav.residents'), icon: <UserMultiple size={16} /> },
    { to: '/antiguos', label: t('nav.formerResidents'), icon: <Archive size={16} /> },
    { to: '/empleados', label: t('nav.employees'), icon: <Group size={16} /> },
  ]

  const nav = (
    <>
      <nav aria-label={t('nav.label')} className="side-nav">
        <ul>
          {items.map((item) => (
            <li key={item.to}>
              <NavLink to={item.to} className="side-nav__link">
                {item.icon}
                <span>{item.label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div className="side-foot">
        <LanguageSwitch />
        <NavLink to="/perfil" className="user-row">
          <span className="avatar" aria-hidden="true">
            {user.name.charAt(0).toUpperCase()}
          </span>
          <span className="user-row__text">
            <span className="user-row__name">{user.name}</span>
            <span className="user-row__meta">{t(`speciality.${user.speciality}`)}</span>
          </span>
          <UserProfile size={16} aria-label={t('nav.profile')} />
        </NavLink>
        <button type="button" className="side-nav__link side-nav__logout" onClick={logout}>
          <Logout size={16} />
          <span>{t('nav.logout')}</span>
        </button>
      </div>
    </>
  )

  return (
    <div className="shell">
      <a className="skip-link" href="#main">
        {t('nav.skipToContent')}
      </a>

      <aside className="sidebar" aria-label={t('app.name')}>
        <Brand />
        {nav}
      </aside>

      {/* Móvil y tablet: barra de 48px con un menú a pantalla completa */}
      <div className="topbar">
        <Brand compact />
        <button
          ref={menuButton}
          type="button"
          className="icon-button"
          aria-expanded={menuOpen}
          aria-controls="mobile-menu"
          aria-label={menuOpen ? t('nav.closeMenu') : t('nav.openMenu')}
          onClick={() => setMenuOpen((open) => !open)}
        >
          {menuOpen ? <Close size={20} /> : <Menu size={20} />}
        </button>
      </div>
      {menuOpen && (
        <div id="mobile-menu" className="mobile-menu">
          {nav}
        </div>
      )}

      <main id="main" className="canvas" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  )
}

function Brand({ compact }: { compact?: boolean }) {
  const { t } = useTranslation()
  return (
    <div className="brand">
      <Mark size={compact ? 24 : 28} />
      <span className="brand__text">
        <span className="brand__name">{t('app.name')}</span>
        {!compact && <span className="brand__scope">{t('app.scopeShort')}</span>}
      </span>
    </div>
  )
}
