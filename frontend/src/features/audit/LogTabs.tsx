import { useTranslation } from 'react-i18next'
import { NavLink } from 'react-router-dom'

// Los dos registros de la administración, uno al lado del otro: los cambios en los datos de los residentes y los
// accesos a la aplicación. Son enlaces (cada uno tiene su URL), con aria-current en el que está abierto
export function LogTabs() {
  const { t } = useTranslation()
  return (
    <nav className="log-tabs" aria-label={t('logs.label')}>
      <NavLink to="/registro" end className="log-tabs__link">{t('logs.changes')}</NavLink>
      <NavLink to="/registro/accesos" className="log-tabs__link">{t('logs.access')}</NavLink>
    </nav>
  )
}
