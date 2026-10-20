import { Button, TextArea, TextInput } from '@carbon/react'
import { useState, type FormEvent, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import type { Patient, PatientDetails } from '../../api/types'
import { todayIso } from '../../shared/dates'
import { ErrorNotice } from '../../shared/ui'

export function emptyDetails(): PatientDetails {
  return {
    name: '',
    surname: '',
    dni: '',
    birthDate: '',
    address: '',
    phoneNumber: '',
    sip: '',
    visitCode: '',
    belongings: '',
    abuseSubstances: '',
    extraInfo: '',
  }
}

export function detailsOf(patient: Patient): PatientDetails {
  return {
    name: patient.name,
    surname: patient.surname,
    dni: patient.dni,
    birthDate: patient.birthDate,
    address: patient.address,
    phoneNumber: patient.phoneNumber,
    sip: patient.sip ?? '',
    visitCode: patient.visitCode ?? '',
    belongings: patient.belongings ?? '',
    abuseSubstances: patient.abuseSubstances ?? '',
    extraInfo: patient.extraInfo ?? '',
  }
}

type Errors = Partial<Record<keyof PatientDetails, string>>

const REQUIRED: (keyof PatientDetails)[] = ['name', 'surname', 'dni', 'birthDate', 'address', 'phoneNumber']

interface Props {
  initial: PatientDetails
  submitLabel: string
  // Campos que solo tiene el alta (la fecha de entrada), pintados al principio del formulario
  extraFields?: ReactNode
  onSubmit: (details: PatientDetails) => Promise<void>
  onCancel: () => void
}

// La API valida de verdad (DNI con letra de control, longitudes, fechas). Aquí solo se comprueba
// lo obligatorio para avisar antes de enviar; el resto de errores llega del servidor y se muestra arriba
export function PatientForm({ initial, submitLabel, extraFields, onSubmit, onCancel }: Props) {
  const { t } = useTranslation()
  const [values, setValues] = useState<PatientDetails>(initial)
  const [errors, setErrors] = useState<Errors>({})
  const [serverError, setServerError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)

  const set = (field: keyof PatientDetails) => (event: { target: { value: string } }) =>
    setValues((v) => ({ ...v, [field]: event.target.value }))

  async function submit(event: FormEvent) {
    event.preventDefault()
    const found: Errors = {}
    for (const field of REQUIRED) {
      if (!values[field].trim()) found[field] = t('form.required')
    }
    setErrors(found)
    if (Object.keys(found).length > 0) {
      // El foco va al primer campo con error, para que teclado y lector de pantalla lleguen a él
      document.getElementById(`patient-${Object.keys(found)[0]}`)?.focus()
      return
    }
    setBusy(true)
    setServerError(null)
    try {
      await onSubmit(values)
    } catch (err) {
      setServerError(err)
      setBusy(false)
      window.scrollTo({ top: 0 })
    }
  }

  const text = (field: keyof PatientDetails, props: { required?: boolean; maxLength?: number; type?: string; helper?: string; autoComplete?: string } = {}) => (
    <TextInput
      id={`patient-${field}`}
      labelText={props.required ? `${t(`patient.${field}`)} (${t('form.requiredMark')})` : t(`patient.${field}`)}
      type={props.type ?? 'text'}
      value={values[field]}
      onChange={set(field)}
      maxLength={props.maxLength}
      helperText={props.helper}
      autoComplete={props.autoComplete ?? 'off'}
      invalid={Boolean(errors[field])}
      invalidText={errors[field]}
      max={props.type === 'date' ? todayIso() : undefined}
    />
  )

  return (
    <form className="form" onSubmit={submit} noValidate>
      <ErrorNotice error={serverError} title={t('form.notSaved')} />

      {extraFields}

      <fieldset className="form-section">
        <legend>{t('patient.sectionPersonal')}</legend>
        <div className="form-grid">
          {text('name', { required: true, maxLength: 50 })}
          {text('surname', { required: true, maxLength: 100 })}
          {text('dni', { required: true, maxLength: 9, helper: t('patient.dniHelp') })}
          {text('birthDate', { required: true, type: 'date' })}
          {text('phoneNumber', { required: true, maxLength: 20, type: 'tel' })}
          {text('sip', { maxLength: 25, helper: t('patient.sipHelp') })}
        </div>
        <div className="form-grid form-grid--full">{text('address', { required: true, maxLength: 500 })}</div>
      </fieldset>

      <fieldset className="form-section">
        <legend>{t('patient.sectionCentre')}</legend>
        <div className="form-grid">
          {text('visitCode', { maxLength: 25, helper: t('patient.visitCodeHelp') })}
          {text('abuseSubstances', { maxLength: 100 })}
        </div>
        <div className="form-grid form-grid--full">
          <TextArea
            id="patient-belongings"
            labelText={t('patient.belongings')}
            value={values.belongings}
            onChange={set('belongings')}
            rows={3}
          />
          <TextArea
            id="patient-extraInfo"
            labelText={t('patient.extraInfo')}
            value={values.extraInfo}
            onChange={set('extraInfo')}
            rows={4}
          />
        </div>
      </fieldset>

      <div className="form-actions">
        <Button kind="secondary" type="button" onClick={onCancel} disabled={busy}>
          {t('common.cancel')}
        </Button>
        <Button type="submit" disabled={busy}>
          {busy ? t('common.saving') : submitLabel}
        </Button>
      </div>
    </form>
  )
}
