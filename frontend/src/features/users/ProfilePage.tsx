import { Button, TextInput } from '@carbon/react'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { changeMyPassword, updateMe } from '../../api/users'
import { useAuth } from '../../auth/AuthContext'
import { ErrorNotice, Field, LanguageSwitch, PageHeader, SuccessNotice } from '../../shared/ui'
import { PasswordForm } from './EmployeeFormPage'
import { useSaveLanguage } from './language'
import { SignatureSection } from './SignatureSection'
import { useTutorial } from '../tutorial/Tutorial'

// RF4: cada empleado cambia sus datos y su contraseña, salvo el email
export function ProfilePage() {
  const { t } = useTranslation()
  const { user, replaceUser } = useAuth()
  const saveLanguage = useSaveLanguage()
  const tutorial = useTutorial()
  const [name, setName] = useState(user?.name ?? '')
  const [phone, setPhone] = useState(user?.phoneNumber ?? '')
  const [errors, setErrors] = useState<{ name?: string; phone?: string }>({})
  const [serverError, setServerError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [saved, setSaved] = useState<string | null>(null)

  if (!user) return null

  async function submit(event: FormEvent) {
    event.preventDefault()
    const found: typeof errors = {}
    if (!name.trim()) found.name = t('form.required')
    if (!phone.trim()) found.phone = t('form.required')
    setErrors(found)
    if (Object.keys(found).length > 0) return
    setBusy(true)
    setServerError(null)
    try {
      replaceUser(await updateMe(name, phone))
      setSaved(t('profile.saved'))
    } catch (err) {
      setServerError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageHeader title={t('profile.title')} subtitle={`${user.email} · ${t(`speciality.${user.speciality}`)}`} />
      <SuccessNotice message={saved} />

      <section className="settings-row" aria-labelledby="my-data-title">
        <div className="settings-row__label">
          <h2 id="my-data-title">{t('profile.data')}</h2>
          <p>{t('profile.dataHelp')}</p>
          <dl className="kv-list">
            <Field label={t('user.email')}>{user.email}</Field>
            <Field label={t('user.speciality')}>{t(`speciality.${user.speciality}`)}</Field>
          </dl>
        </div>
        <div className="settings-row__panel">
          <form className="form form--compact" onSubmit={submit} noValidate>
            <ErrorNotice error={serverError} title={t('form.notSaved')} />
            <TextInput id="me-name" labelText={t('user.name')} value={name} onChange={(e) => setName(e.target.value)} maxLength={50} invalid={Boolean(errors.name)} invalidText={errors.name} autoComplete="name" />
            <TextInput id="me-phone" type="tel" maxLength={20} labelText={t('user.phoneNumber')} value={phone} onChange={(e) => setPhone(e.target.value)} invalid={Boolean(errors.phone)} invalidText={errors.phone} autoComplete="tel" />
            <div className="form-actions">
              <Button type="submit" disabled={busy}>
                {busy ? t('common.saving') : t('common.save')}
              </Button>
            </div>
          </form>
        </div>
      </section>

      <section className="settings-row" aria-labelledby="my-password-title">
        <div className="settings-row__label">
          <h2 id="my-password-title">{t('profile.password')}</h2>
          <p>{t('profile.passwordHelp')}</p>
        </div>
        <div className="settings-row__panel">
          <PasswordForm
            idPrefix="me"
            withCurrent
            submitLabel={t('profile.changePassword')}
            onSubmit={async (newPassword, currentPassword) => {
              await changeMyPassword(currentPassword, newPassword)
              setSaved(t('profile.passwordChanged'))
            }}
          />
        </div>
      </section>

      {/* Memoria final: "Para cambiar de idioma el empleado deberá acceder a su perfil" */}
      <section className="settings-row" aria-labelledby="my-language-title">
        <div className="settings-row__label">
          <h2 id="my-language-title">{t('profile.language.title')}</h2>
          <p>{t('profile.language.help')}</p>
        </div>
        <div className="settings-row__panel">
          <LanguageSwitch onChange={saveLanguage} />
        </div>
      </section>

      <SignatureSection />

      <section className="settings-row" aria-labelledby="my-tutorial-title">
        <div className="settings-row__label">
          <h2 id="my-tutorial-title">{t('profile.tutorial.title')}</h2>
          <p>{t('profile.tutorial.help')}</p>
        </div>
        <div className="settings-row__panel">
          <Button kind="tertiary" onClick={tutorial.start} disabled={tutorial.active}>
            {t('profile.tutorial.start')}
          </Button>
        </div>
      </section>
    </>
  )
}
