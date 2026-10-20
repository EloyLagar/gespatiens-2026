import { Button, InlineNotification, PasswordInput, Stack, TextInput } from '@carbon/react'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { ErrorNotice, LanguageSwitch, Mark } from '../../shared/ui'

export function LoginPage() {
  const { t } = useTranslation()
  const { user, login, endedBecause } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  const from = (location.state as { from?: string } | null)?.from ?? '/avisos'
  if (user) return <Navigate to={from} replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(email, password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err)
      setBusy(false)
    }
  }

  return (
    <main className="login">
      <div className="login__panel">
        <div className="login__brand">
          <Mark size={40} />
          <div>
            <p className="login__name">{t('app.name')}</p>
            <p className="login__scope">{t('app.scope')}</p>
          </div>
        </div>

        <h1>{t('login.title')}</h1>
        <p className="login__lead">{t('login.lead')}</p>

        {endedBecause === 'expired' && (
          <div role="status">
            <InlineNotification kind="info" lowContrast hideCloseButton title={t('login.expired')} />
          </div>
        )}

        <form onSubmit={submit} noValidate>
          <Stack gap={6}>
            <TextInput
              id="email"
              type="email"
              autoComplete="username"
              labelText={t('login.email')}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <PasswordInput
              id="password"
              autoComplete="current-password"
              labelText={t('login.password')}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              showPasswordLabel={t('login.showPassword')}
              hidePasswordLabel={t('login.hidePassword')}
              required
            />
            <ErrorNotice error={error} title={t('login.failed')} />
            <Button type="submit" disabled={busy} className="login__submit">
              {busy ? t('login.entering') : t('login.submit')}
            </Button>
          </Stack>
        </form>

        <p className="login__help">{t('login.forgot')}</p>
        <LanguageSwitch />
      </div>
    </main>
  )
}
