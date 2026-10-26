import { useEffect, useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { demoAccounts, type DemoAccount } from '../../api/demo'
import { useAuth } from '../../auth/AuthContext'

// Solo en la demo (y al desarrollar con el perfil demo): pasar a otra cuenta sin cerrar la sesión, para ver la
// aplicación como cada perfil. No es una suplantación de verdad: entra con la contraseña de esa cuenta, la que ya
// publica el README. En un centro de verdad la API no da cuentas y aquí no sale nada
export function DemoUserSwitch() {
  const { t } = useTranslation()
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const id = useId()
  const [accounts, setAccounts] = useState<DemoAccount[]>([])
  const [busy, setBusy] = useState(false)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let active = true
    demoAccounts()
      .then((found) => active && setAccounts(found))
      .catch(() => {})
    return () => {
      active = false
    }
  }, [])

  if (!user || accounts.length === 0) return null

  async function switchTo(email: string) {
    const account = accounts.find((candidate) => candidate.email === email)
    if (!account) return
    setBusy(true)
    setFailed(false)
    try {
      await login(account.email, account.password)
      // La pantalla de ahora puede ser solo de administración: se empieza por los avisos, como al entrar
      navigate('/avisos')
    } catch {
      setFailed(true)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="demo-switch">
      <label htmlFor={id}>{t('demoSwitch.label')}</label>
      <select id={id} value={user.email} disabled={busy} onChange={(event) => switchTo(event.target.value)}>
        {accounts.map((account) => (
          <option key={account.email} value={account.email}>
            {account.name} · {t(`speciality.${account.speciality}`)}
          </option>
        ))}
      </select>
      {failed && (
        <p className="demo-switch__error" role="alert">
          {t('demoSwitch.failed')}
        </p>
      )}
    </div>
  )
}
