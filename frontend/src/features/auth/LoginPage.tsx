import { Button, InlineNotification, PasswordInput, Stack, TextInput } from '@carbon/react'
import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { demoAccounts, type DemoAccount } from '../../api/demo'
import { useAuth } from '../../auth/AuthContext'
import { ErrorNotice, LanguageSwitch, Wordmark } from '../../shared/ui'

export function LoginPage() {
  const { t } = useTranslation()
  const { user, login, endedBecause } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  // Al volver de crear la contraseña desde el correo, el email ya viene puesto
  const created = (location.state as { passwordCreatedFor?: string } | null)?.passwordCreatedFor
  const [email, setEmail] = useState(created ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  // La demo publicada: las cuentas para entrar con un clic. Vacío en un centro de verdad
  const [accounts, setAccounts] = useState<DemoAccount[]>([])
  // Mientras no responde la API. En el plan gratuito el servidor de la demo se duerme y tarda en despertar
  const [connecting, setConnecting] = useState(true)
  const slow = useSlow(connecting || busy)

  // Pedir las cuentas también despierta al servidor mientras se escribe el email
  useEffect(() => {
    let active = true
    demoAccounts()
      .then((found) => active && setAccounts(found))
      .catch(() => {})
      .finally(() => active && setConnecting(false))
    return () => {
      active = false
    }
  }, [])

  const from = (location.state as { from?: string } | null)?.from ?? '/avisos'
  if (user) return <Navigate to={from} replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    await enter(email, password)
  }

  async function enter(withEmail: string, withPassword: string) {
    setBusy(true)
    setError(null)
    try {
      await login(withEmail, withPassword)
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
          <div>
            <p className="login__name"><Wordmark size={32} /></p>
            <p className="login__scope">{t('app.scope')}</p>
          </div>
        </div>

        <h1>{t('login.title')}</h1>
        <p className="login__lead">{t('login.lead')}</p>

        {created && (
          <div role="status">
            <InlineNotification kind="success" lowContrast hideCloseButton title={t('passwordSetup.done')} />
          </div>
        )}

        {endedBecause === 'expired' && (
          <div role="status">
            <InlineNotification kind="info" lowContrast hideCloseButton title={t('login.expired')} />
          </div>
        )}

        {slow && (
          <div role="status">
            <InlineNotification kind="info" lowContrast hideCloseButton title={t('login.waking')} />
          </div>
        )}

        {accounts.length > 0 && (
          <section className="login__demo" aria-labelledby="demo-title">
            <h2 id="demo-title">{t('login.demoTitle')}</h2>
            <p>{t('login.demoLead')}</p>
            <ul>
              {accounts.map((account) => (
                <li key={account.email}>
                  <Button
                    kind="tertiary"
                    size="md"
                    disabled={busy}
                    onClick={() => enter(account.email, account.password)}
                  >
                    <span>
                      {t('login.demoAs', { speciality: t(`speciality.${account.speciality}`) })}
                      <span className="login__demo-name">{account.name}</span>
                    </span>
                  </Button>
                </li>
              ))}
            </ul>
            <p className="login__demo-or">{t('login.demoOr')}</p>
          </section>
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
        <div className="login__foot">
          <LanguageSwitch />
          <Link to="/accesibilidad">{t('accessibility.link')}</Link>
        </div>
      </div>
    </main>
  )
}

// Solo avisa si de verdad tarda: con el servidor despierto la respuesta llega antes y el aviso no parpadea
function useSlow(pending: boolean, after = 2500) {
  const [slow, setSlow] = useState(false)
  useEffect(() => {
    if (!pending) {
      setSlow(false)
      return
    }
    const timer = setTimeout(() => setSlow(true), after)
    return () => clearTimeout(timer)
  }, [pending, after])
  return slow
}
