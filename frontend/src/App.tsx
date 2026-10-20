import { useTranslation } from 'react-i18next'
import { Link, Navigate, Route, Routes } from 'react-router-dom'
import { RequireAdmin, RequireAuth } from './auth/RequireAuth'
import { LoginPage } from './features/auth/LoginPage'
import { DiaryPage } from './features/diary/DiaryPage'
import { NoticesPage } from './features/notices/NoticesPage'
import { AdmitPatientPage } from './features/patients/AdmitPatientPage'
import { FormerResidentsPage } from './features/patients/FormerResidentsPage'
import { PatientDetailPage } from './features/patients/PatientDetailPage'
import { ResidentsPage } from './features/patients/ResidentsPage'
import { EmployeeFormPage } from './features/users/EmployeeFormPage'
import { EmployeesPage } from './features/users/EmployeesPage'
import { ProfilePage } from './features/users/ProfilePage'
import { AppShell } from './layout/AppShell'
import { PageHeader } from './shared/ui'

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        {/* La memoria pone los avisos en la página principal */}
        <Route index element={<Navigate to="/avisos" replace />} />
        <Route path="avisos" element={<NoticesPage />} />
        <Route path="diario" element={<DiaryPage />} />
        <Route path="diario/:date" element={<DiaryPage />} />
        <Route path="residentes" element={<ResidentsPage />} />
        <Route path="residentes/nuevo" element={<AdmitPatientPage />} />
        <Route path="residentes/:id" element={<PatientDetailPage />} />
        <Route path="antiguos" element={<FormerResidentsPage />} />
        <Route path="empleados" element={<EmployeesPage />} />
        <Route
          path="empleados/nuevo"
          element={
            <RequireAdmin>
              <EmployeeFormPage />
            </RequireAdmin>
          }
        />
        <Route
          path="empleados/:id"
          element={
            <RequireAdmin>
              <EmployeeFormPage />
            </RequireAdmin>
          }
        />
        <Route path="perfil" element={<ProfilePage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}

function NotFound() {
  const { t } = useTranslation()
  return (
    <>
      <PageHeader title={t('notFound.title')} subtitle={t('notFound.text')} />
      <Link to="/avisos">{t('notFound.back')}</Link>
    </>
  )
}
