import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { checkPasswordSetup, completePasswordSetup } from '../../api/auth'
import { PasswordForm } from '../users/EmployeeFormPage'
import { ErrorNotice, LanguageSwitch, LoadingRows, Wordmark } from '../../shared/ui'
import { useApi } from '../../shared/useApi'

// Memoria final: el empleado nuevo recibe un correo y desde su enlace crea sus credenciales de acceso.
// También lo usa quien ha olvidado la contraseña y ha pedido un enlace a la administración
export function PasswordSetupPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const { data: account, error, loading } = useApi(
    () => (token ? checkPasswordSetup(token) : Promise.reject(new Error('sin token'))),
    [token],
  )

  return (
    <main className="login">
      <div className="login__panel">
        <div className="login__brand">
          <div>
            <p className="login__name"><Wordmark size={32} /></p>
            <p className="login__scope">{t('app.scope')}</p>
          </div>
        </div>

        <h1>{t('passwordSetup.title')}</h1>

        {loading && !account && !error ? (
          <LoadingRows rows={3} label={t('common.loading')} />
        ) : error || !account ? (
          <>
            {/* El enlace no existe, ya se usó, lo sustituyó otro o caducó: siempre el mismo mensaje */}
            <ErrorNotice error={token ? error : null} title={t('passwordSetup.invalidTitle')} />
            <p className="login__lead">{t('passwordSetup.invalid')}</p>
            <Link to="/login">{t('passwordSetup.toLogin')}</Link>
          </>
        ) : (
          <>
            <p className="login__lead">{t('passwordSetup.lead', { name: account.name, email: account.email })}</p>
            <PasswordForm
              idPrefix="setup"
              primary
              submitLabel={t('passwordSetup.submit')}
              onSubmit={async (newPassword) => {
                await completePasswordSetup(token, newPassword)
                navigate('/login', { replace: true, state: { passwordCreatedFor: account.email } })
              }}
            />
          </>
        )}

        <LanguageSwitch />
      </div>
    </main>
  )
}
