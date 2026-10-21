import { Button, InlineNotification, PasswordInput, Select, SelectItem, Tag, TextInput } from '@carbon/react'
import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { SPECIALITIES, type Speciality, type User } from '../../api/types'
import { deleteUser, editUser, getUser, registerUser, resetUserPassword, sendPasswordSetup } from '../../api/users'
import { ConfirmDialog, ErrorNotice, LoadingRows, PageHeader, SuccessNotice } from '../../shared/ui'
import { useApi } from '../../shared/useApi'

// Debe coincidir con PasswordPolicy.MIN_PASSWORD_LENGTH en Spring
export const MIN_PASSWORD_LENGTH = 8

interface Values {
  name: string
  email: string
  phoneNumber: string
  speciality: Speciality
}

// Tras el alta, la ficha del empleado dice si el correo para crear la contraseña ha salido
type Flash = 'invited' | 'inviteFailed' | null

// RF7: alta (/empleados/nuevo) y edición (/empleados/:id) de empleados por la administración
export function EmployeeFormPage() {
  const { id } = useParams()
  return id ? <EditEmployee id={id} /> : <NewEmployee />
}

function NewEmployee() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  return (
    <>
      <PageHeader title={t('employeeForm.newTitle')} breadcrumb={<Link to="/empleados">{t('nav.employees')}</Link>} />
      <p className="form-intro">{t('employeeForm.inviteHelp')}</p>
      <EmployeeForm
        submitLabel={t('employeeForm.create')}
        onSubmit={async (v) => {
          const { user, emailSent } = await registerUser(v)
          navigate(`/empleados/${user.id}`, { state: { flash: emailSent ? 'invited' : 'inviteFailed' } })
        }}
      />
    </>
  )
}

function EditEmployee({ id }: { id: string }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const { data, error, loading, reload } = useApi(() => getUser(id), [id])
  const [user, setUser] = useState<User | null>(null)
  const [flash] = useState<Flash>((location.state as { flash?: Flash } | null)?.flash ?? null)
  const [saved, setSaved] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const [sendError, setSendError] = useState<unknown>(null)
  const [deleting, setDeleting] = useState(false)
  const [busy, setBusy] = useState(false)
  const [deleteError, setDeleteError] = useState<unknown>(null)

  useEffect(() => setUser(data), [data])
  // El aviso del alta llega en el estado de la navegación: se borra para que no vuelva a salir al recargar
  useEffect(() => {
    if (location.state) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  if (error) {
    return (
      <>
        <PageHeader title={t('employeeForm.editTitleUnknown')} breadcrumb={<Link to="/empleados">{t('nav.employees')}</Link>} />
        <ErrorNotice error={error} onRetry={reload} />
      </>
    )
  }
  if (loading || !user) return <LoadingRows rows={5} label={t('common.loading')} />

  return (
    <>
      <PageHeader title={user.name} subtitle={t(`speciality.${user.speciality}`)} breadcrumb={<Link to="/empleados">{t('nav.employees')}</Link>} />
      <SuccessNotice message={saved ?? (flash === 'invited' ? t('employeeForm.invited', { email: user.email }) : null)} />
      {flash === 'inviteFailed' && !saved && (
        <div role="alert">
          <InlineNotification kind="warning" lowContrast hideCloseButton title={t('employeeForm.inviteFailedTitle')} subtitle={t('employeeForm.inviteFailed')} />
        </div>
      )}

      <section className="settings-row" aria-labelledby="profile-title">
        <div className="settings-row__label">
          <h2 id="profile-title">{t('employeeForm.profile')}</h2>
          <p>{t('employeeForm.profileHelp')}</p>
        </div>
        <div className="settings-row__panel">
          <EmployeeForm
            key={user.id + user.email + user.speciality}
            initial={user}
            submitLabel={t('common.save')}
            onSubmit={async (v) => {
              setUser(await editUser(user.id, v))
              setSaved(t('employeeForm.saved'))
            }}
          />
        </div>
      </section>

      <section className="settings-row" aria-labelledby="reset-title">
        <div className="settings-row__label">
          <h2 id="reset-title">{t('employeeForm.resetTitle')}</h2>
          <p>{user.passwordPending ? t('employeeForm.pendingHelp') : t('employeeForm.linkHelp')}</p>
        </div>
        <div className="settings-row__panel">
          <div className="form form--compact">
            {user.passwordPending && (
              <p>
                <Tag type="warm-gray" size="sm">{t('employees.pending')}</Tag>
              </p>
            )}
            <ErrorNotice error={sendError} title={t('employeeForm.linkNotSent')} />
            <div className="form-actions">
              <Button
                kind="secondary"
                disabled={sending}
                onClick={async () => {
                  setSending(true)
                  setSendError(null)
                  try {
                    await sendPasswordSetup(user.id)
                    setSaved(t('employeeForm.linkSent', { email: user.email }))
                  } catch (err) {
                    setSendError(err)
                  } finally {
                    setSending(false)
                  }
                }}
              >
                {sending ? t('employeeForm.sending') : user.passwordPending ? t('employeeForm.resendInvite') : t('employeeForm.sendLink')}
              </Button>
            </div>
          </div>
          <h3 className="settings-row__subtitle">{t('employeeForm.resetManualTitle')}</h3>
          <p className="settings-row__hint">{t('employeeForm.resetHelp')}</p>
          <PasswordForm
            idPrefix="reset"
            submitLabel={t('employeeForm.resetSubmit')}
            onSubmit={async (newPassword) => {
              await resetUserPassword(user.id, newPassword)
              setUser({ ...user, passwordPending: false })
              setSaved(t('employeeForm.resetDone', { name: user.name }))
            }}
          />
        </div>
      </section>

      <section className="danger-zone" aria-labelledby="delete-user-title">
        <h2 id="delete-user-title">{t('employeeForm.deleteTitle')}</h2>
        <p>{t('employeeForm.deleteText')}</p>
        <Button kind="danger--tertiary" onClick={() => setDeleting(true)}>
          {t('employeeForm.delete')}
        </Button>
      </section>

      <ConfirmDialog
        open={deleting}
        danger
        title={t('employeeForm.deleteConfirmTitle', { name: user.name })}
        confirmLabel={t('employeeForm.deleteConfirm')}
        busy={busy}
        error={deleteError}
        onClose={() => {
          setDeleting(false)
          setDeleteError(null)
        }}
        onConfirm={async () => {
          setBusy(true)
          setDeleteError(null)
          try {
            await deleteUser(user.id)
            navigate('/empleados', { replace: true })
          } catch (err) {
            setDeleteError(err)
            setBusy(false)
          }
        }}
      >
        <p>{t('employeeForm.deleteBody', { name: user.name, email: user.email })}</p>
      </ConfirmDialog>
    </>
  )
}

interface FormProps {
  initial?: User
  submitLabel: string
  onSubmit: (values: Values) => Promise<void>
}

function EmployeeForm({ initial, submitLabel, onSubmit }: FormProps) {
  const { t } = useTranslation()
  const [values, setValues] = useState<Values>({
    name: initial?.name ?? '',
    email: initial?.email ?? '',
    phoneNumber: initial?.phoneNumber ?? '',
    speciality: initial?.speciality ?? 'SOCIAL_EDUCATOR',
  })
  const [errors, setErrors] = useState<Partial<Record<keyof Values, string>>>({})
  const [serverError, setServerError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  const set = (field: keyof Values) => (e: { target: { value: string } }) => setValues((v) => ({ ...v, [field]: e.target.value }))

  async function submit(event: FormEvent) {
    event.preventDefault()
    const found: Partial<Record<keyof Values, string>> = {}
    if (!values.name.trim()) found.name = t('form.required')
    if (!values.email.trim()) found.email = t('form.required')
    if (!values.phoneNumber.trim()) found.phoneNumber = t('form.required')
    setErrors(found)
    if (Object.keys(found).length > 0) {
      document.getElementById(`employee-${Object.keys(found)[0]}`)?.focus()
      return
    }
    setBusy(true)
    setServerError(null)
    try {
      await onSubmit(values)
    } catch (err) {
      setServerError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="form form--compact" onSubmit={submit} noValidate>
      <ErrorNotice error={serverError} title={t('form.notSaved')} />
      <TextInput id="employee-name" labelText={`${t('user.name')} (${t('form.requiredMark')})`} value={values.name} onChange={set('name')} maxLength={50} invalid={Boolean(errors.name)} invalidText={errors.name} autoComplete="off" />
      <TextInput id="employee-email" type="email" labelText={`${t('user.email')} (${t('form.requiredMark')})`} value={values.email} onChange={set('email')} invalid={Boolean(errors.email)} invalidText={errors.email} helperText={t('employeeForm.emailHelp')} autoComplete="off" />
      <TextInput id="employee-phoneNumber" type="tel" labelText={`${t('user.phoneNumber')} (${t('form.requiredMark')})`} value={values.phoneNumber} onChange={set('phoneNumber')} invalid={Boolean(errors.phoneNumber)} invalidText={errors.phoneNumber} autoComplete="off" />
      <Select id="employee-speciality" labelText={t('user.speciality')} value={values.speciality} onChange={set('speciality')} helperText={t('employeeForm.specialityHelp')}>
        {SPECIALITIES.map((s) => (
          <SelectItem key={s} value={s} text={t(`speciality.${s}`)} />
        ))}
      </Select>
      <div className="form-actions">
        <Button type="submit" disabled={busy}>
          {busy ? t('common.saving') : submitLabel}
        </Button>
      </div>
    </form>
  )
}

interface PasswordFormProps {
  idPrefix: string
  // Cambiar la propia contraseña exige la actual (RF4); restablecer la de otro no
  withCurrent?: boolean
  // Botón principal cuando el formulario es lo único de la pantalla (crear la contraseña desde el correo)
  primary?: boolean
  submitLabel: string
  onSubmit: (newPassword: string, currentPassword: string) => Promise<void>
}

export function PasswordForm({ idPrefix, withCurrent, primary, submitLabel, onSubmit }: PasswordFormProps) {
  const { t } = useTranslation()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [repeat, setRepeat] = useState('')
  const [errors, setErrors] = useState<{ current?: string; next?: string; repeat?: string }>({})
  const [serverError, setServerError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    const found: typeof errors = {}
    if (withCurrent && !current) found.current = t('form.required')
    if (next.length < MIN_PASSWORD_LENGTH) found.next = t('form.passwordLength', { min: MIN_PASSWORD_LENGTH })
    else if (next !== repeat) found.repeat = t('form.passwordMismatch')
    setErrors(found)
    if (Object.keys(found).length > 0) {
      document.getElementById(`${idPrefix}-${Object.keys(found)[0]}`)?.focus()
      return
    }
    setBusy(true)
    setServerError(null)
    try {
      await onSubmit(next, current)
      setCurrent('')
      setNext('')
      setRepeat('')
    } catch (err) {
      setServerError(err)
    } finally {
      setBusy(false)
    }
  }

  const labels = { showPasswordLabel: t('login.showPassword'), hidePasswordLabel: t('login.hidePassword') }
  return (
    <form className="form form--compact" onSubmit={submit} noValidate>
      <ErrorNotice error={serverError} title={t('form.notSaved')} />
      {withCurrent && (
        <PasswordInput id={`${idPrefix}-current`} labelText={t('password.current')} value={current} onChange={(e) => setCurrent(e.target.value)} invalid={Boolean(errors.current)} invalidText={errors.current} autoComplete="current-password" {...labels} />
      )}
      <PasswordInput id={`${idPrefix}-next`} labelText={t('password.new')} helperText={t('form.passwordLength', { min: MIN_PASSWORD_LENGTH })} value={next} onChange={(e) => setNext(e.target.value)} invalid={Boolean(errors.next)} invalidText={errors.next} autoComplete="new-password" {...labels} />
      <PasswordInput id={`${idPrefix}-repeat`} labelText={t('password.repeat')} value={repeat} onChange={(e) => setRepeat(e.target.value)} invalid={Boolean(errors.repeat)} invalidText={errors.repeat} autoComplete="new-password" {...labels} />
      <div className="form-actions">
        <Button type="submit" kind={primary ? 'primary' : 'secondary'} disabled={busy}>
          {busy ? t('common.saving') : submitLabel}
        </Button>
      </div>
    </form>
  )
}
