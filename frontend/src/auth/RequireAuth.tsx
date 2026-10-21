import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { PageHeader } from '../shared/ui'
import { useAuth } from './AuthContext'

// RF3: sin sesión no se ve nada de la aplicación
export function RequireAuth({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return children
}

// RNF3: lo que un empleado no puede hacer no se le muestra. La API lo vuelve a comprobar (403)
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { isAdmin } = useAuth()
  const { t } = useTranslation()
  // Quien llega con un enlace a una pantalla de la administración ve por qué no puede entrar, en vez de
  // aparecer en otra pantalla sin explicación
  if (!isAdmin) {
    return (
      <>
        <PageHeader title={t('forbidden.title')} subtitle={t('forbidden.text')} />
        <Link to="/avisos">{t('notFound.back')}</Link>
      </>
    )
  }
  return children
}
