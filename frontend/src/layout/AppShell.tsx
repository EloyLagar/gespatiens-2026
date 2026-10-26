import { Archive, Calendar, ChartLine, Close, Dashboard, Collaborate, Document, Group, Logout, Menu, Notebook, Notification, RecentlyViewed, SidePanelClose, SidePanelOpen, UserMultiple, UserProfile } from '@carbon/icons-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { LanguageSwitch, Wordmark } from '../shared/ui'
import { useSaveLanguage } from '../features/users/language'
import { useTutorial } from '../features/tutorial/Tutorial'

// En el escritorio el menú se puede plegar a una columna de iconos, para dar ancho a las tablas
// grandes (el cuadrante, las valoraciones). Se recuerda en este navegador
const RAIL_KEY = 'gespatiens.sidebar'

function loadRail(): boolean {
  try {
    return localStorage.getItem(RAIL_KEY) === 'rail'
  } catch {
    return false
  }
}

function saveRail(rail: boolean) {
  try {
    if (rail) localStorage.setItem(RAIL_KEY, 'rail')
    else localStorage.removeItem(RAIL_KEY)
  } catch {
    // Sin almacenamiento: se pliega solo hasta recargar
  }
}

interface NavItem {
  to: string
  label: string
  icon: ReactNode
}

export function AppShell() {
  const { t } = useTranslation()
  const { user, isAdmin, logout } = useAuth()
  const saveLanguage = useSaveLanguage()
  const tutorial = useTutorial()
  const [menuOpen, setMenuOpen] = useState(false)
  const [rail, setRail] = useState(loadRail)
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
    { to: '/informes', label: t('nav.reports'), icon: <Document size={16} /> },
    { to: '/valoraciones', label: t('nav.evaluations'), icon: <ChartLine size={16} /> },
    { to: '/turnos', label: t('nav.rota'), icon: <Calendar size={16} /> },
    { to: '/intervenciones', label: t('nav.interventions'), icon: <Collaborate size={16} /> },
    { to: '/residentes', label: t('nav.residents'), icon: <UserMultiple size={16} /> },
    { to: '/antiguos', label: t('nav.formerResidents'), icon: <Archive size={16} /> },
    { to: '/empleados', label: t('nav.employees'), icon: <Group size={16} /> },
    // Memoria final, 5.1: el panel de control solo lo ve la administración
    ...(isAdmin ? [{ to: '/panel', label: t('nav.dashboard'), icon: <Dashboard size={16} /> }] : []),
    // RNF7: quién ha consultado o cambiado los datos de cada residente
    ...(isAdmin ? [{ to: '/registro', label: t('nav.audit'), icon: <RecentlyViewed size={16} /> }] : []),
  ]

  const nav = (
    <>
      <nav aria-label={t('nav.label')} className="side-nav">
        <ul>
          {items.map((item) => (
            <li key={item.to}>
              <NavLink to={item.to} className="side-nav__link" title={rail ? item.label : undefined}>
                {item.icon}
                <span>{item.label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div className="side-foot">
        <LanguageSwitch onChange={saveLanguage} />
        <NavLink to="/perfil" className="user-row" title={rail ? t('nav.profile') : undefined}>
          <span className="avatar" aria-hidden="true">
            {user.name.charAt(0).toUpperCase()}
          </span>
          <span className="user-row__text">
            <span className="user-row__name">{user.name}</span>
            <span className="user-row__meta">{t(`speciality.${user.speciality}`)}</span>
          </span>
          <UserProfile size={16} aria-label={t('nav.profile')} />
        </NavLink>
        <button type="button" className="side-nav__link side-nav__logout" onClick={logout} title={rail ? t('nav.logout') : undefined}>
          <Logout size={16} />
          <span>{t('nav.logout')}</span>
        </button>
      </div>
    </>
  )

  return (
    // Con el tutorial abierto, el final de la pantalla deja sitio a su tarjeta
    <div className={['shell', rail && 'shell--rail', tutorial.active && 'shell--touring'].filter(Boolean).join(' ')}>
      <a className="skip-link" href="#main">
        {t('nav.skipToContent')}
      </a>

      <aside className="sidebar" aria-label={t('app.name')}>
        <div className="sidebar__band">
          {!rail && <Brand />}
          <button
            type="button"
            className="icon-button sidebar__toggle"
            aria-expanded={!rail}
            aria-label={rail ? t('nav.expandMenu') : t('nav.collapseMenu')}
            title={rail ? t('nav.expandMenu') : t('nav.collapseMenu')}
            onClick={() => {
              saveRail(!rail)
              setRail(!rail)
            }}
          >
            {rail ? <SidePanelOpen size={20} /> : <SidePanelClose size={20} />}
          </button>
        </div>
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
      <span className="brand__text">
        <span className="brand__name"><Wordmark size={compact ? 22 : 26} /></span>
        {!compact && <span className="brand__scope">{t('app.scopeShort')}</span>}
      </span>
    </div>
  )
}
