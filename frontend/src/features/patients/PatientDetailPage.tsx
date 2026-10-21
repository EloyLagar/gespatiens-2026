import { Button, Tag, TextInput } from '@carbon/react'
import { Edit } from '@carbon/icons-react'
import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { deletePatient, dischargePatient, getPatient, listPatients, updatePatient } from '../../api/patients'
import type { Patient } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { ageOn, daysBetween, formatDate, todayIso } from '../../shared/dates'
import { ConfirmDialog, ErrorNotice, Field, LoadingRows, PageHeader, SuccessNotice } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { ResidentReportsPanel } from '../reports/ResidentReportsPanel'
import { bedsOf, nextFreeNumber } from './beds'
import { detailsOf, PatientForm } from './PatientForm'
import { ReadmitDialog } from './ReadmitDialog'
import { ReferralPanel } from './ReferralPanel'
import { VisitorsPanel } from './VisitorsPanel'

type Flash = 'admitted' | 'readmitted' | 'saved' | 'discharged' | null

export function PatientDetailPage() {
  const { id = '' } = useParams()
  const { t, i18n } = useTranslation()
  const { isAdmin } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { data: loaded, error, loading, reload } = useApi(() => getPatient(id), [id])
  const active = useApi(() => listPatients('ACTIVE'), [id])

  const [patient, setPatient] = useState<Patient | null>(null)
  const [editing, setEditing] = useState(false)
  const [flash, setFlash] = useState<Flash>((location.state as { flash?: Flash } | null)?.flash ?? null)
  const [dialog, setDialog] = useState<'discharge' | 'readmit' | 'delete' | null>(null)
  const [exitDate, setExitDate] = useState(todayIso())
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<unknown>(null)

  useEffect(() => setPatient(loaded), [loaded])
  // El aviso de "dado de alta" llega en el estado de la navegación: se borra para que no
  // vuelva a salir al recargar la página
  useEffect(() => {
    if (location.state) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])
  const nextNumber = useMemo(() => (active.data ? nextFreeNumber(bedsOf(active.data)) : null), [active.data])

  if (error) {
    return (
      <>
        <PageHeader title={t('detail.notFoundTitle')} breadcrumb={<Link to="/residentes">{t('nav.residents')}</Link>} />
        <ErrorNotice error={error} onRetry={reload} />
      </>
    )
  }
  if (loading || !patient) {
    return <LoadingRows rows={6} label={t('common.loading')} />
  }

  const fullName = `${patient.name} ${patient.surname}`
  const parent = patient.active ? { to: '/residentes', label: t('nav.residents') } : { to: '/antiguos', label: t('nav.formerResidents') }

  function closeDialog() {
    setDialog(null)
    setActionError(null)
  }

  async function run(action: () => Promise<void>) {
    setBusy(true)
    setActionError(null)
    try {
      await action()
    } catch (err) {
      setActionError(err)
    } finally {
      setBusy(false)
    }
  }

  if (editing) {
    return (
      <>
        <PageHeader
          title={t('detail.editTitle', { name: fullName })}
          breadcrumb={<Link to={parent.to}>{parent.label}</Link>}
        />
        <PatientForm
          initial={detailsOf(patient)}
          submitLabel={t('common.save')}
          onSubmit={async (details) => {
            setPatient(await updatePatient(patient.id, details))
            setEditing(false)
            setFlash('saved')
          }}
          onCancel={() => setEditing(false)}
        />
      </>
    )
  }

  const status = patient.active ? (
    <Tag type="blue" size="md">
      {t('detail.statusActive', { number: patient.number })}
    </Tag>
  ) : (
    <Tag type="gray" size="md">
      {t('detail.statusFormer', { date: formatDate(patient.exitDate, i18n.language) })}
    </Tag>
  )

  return (
    <>
      <PageHeader
        title={fullName}
        breadcrumb={<Link to={parent.to}>{parent.label}</Link>}
        subtitle={status}
        actions={
          <>
            {/* Memoria final: la ficha la gestiona la administración; los demás la consultan */}
            {isAdmin && (
              <Button kind="tertiary" renderIcon={Edit} onClick={() => setEditing(true)}>
                {t('common.edit')}
              </Button>
            )}
            <Link className="cds--btn cds--btn--ghost" to={`/intervenciones/${patient.id}`}>
              {t('detail.interventions')}
            </Link>
            {/* RF9: la baja es cosa de la administración. Al resto ni se le muestra el botón */}
            {patient.active && isAdmin && (
              <Button kind="danger--tertiary" onClick={() => setDialog('discharge')}>
                {t('detail.discharge')}
              </Button>
            )}
            {!patient.active && isAdmin && (
              <Button onClick={() => setDialog('readmit')} disabled={active.data !== null && nextNumber === null}>
                {t('former.readmit')}
              </Button>
            )}
          </>
        }
      />

      <SuccessNotice message={flash ? t(`detail.flash.${flash}`, { name: fullName, number: patient.number }) : null} />
      {!patient.active && active.data && nextNumber === null && <p className="notice">{t('former.fullNotice')}</p>}

      {/* Franjas a todo el ancho: ninguna fila de paneles con alturas distintas */}
      <section className="panel" aria-labelledby="stay-title">
        <h2 id="stay-title">{t('detail.stay')}</h2>
        <dl className="kv-list kv-list--four">
          <Field label={t('patient.number')} numeric>{patient.number}</Field>
          <Field label={t('patient.entryDate')} numeric>{formatDate(patient.entryDate, i18n.language)}</Field>
          <Field label={t('patient.exitDate')} numeric>{patient.exitDate ? formatDate(patient.exitDate, i18n.language) : null}</Field>
          <Field label={patient.active ? t('patient.daysIn') : t('patient.stay')} numeric>
            {t('detail.days', { count: daysBetween(patient.entryDate, patient.exitDate ?? undefined) })}
          </Field>
        </dl>
      </section>

      <section className="panel" aria-labelledby="personal-title">
        <h2 id="personal-title">{t('patient.sectionPersonal')}</h2>
        <dl className="kv-list kv-list--four">
          <Field label={t('patient.dni')}>{patient.dni}</Field>
          <Field label={t('patient.birthDate')} numeric>
            {formatDate(patient.birthDate, i18n.language)} · {t('patient.ageValue', { count: ageOn(patient.birthDate) })}
          </Field>
          <Field label={t('patient.phoneNumber')}>{patient.phoneNumber}</Field>
          <Field label={t('patient.sip')}>{patient.sip}</Field>
          <Field label={t('patient.address')} span="all">{patient.address}</Field>
        </dl>
      </section>

      <section className="panel" aria-labelledby="centre-title">
        <h2 id="centre-title">{t('patient.sectionCentre')}</h2>
        <dl className="kv-list kv-list--four">
          <Field label={t('patient.visitCode')}>{patient.visitCode}</Field>
          <Field label={t('patient.abuseSubstances')}>{patient.abuseSubstances}</Field>
          <Field label={t('patient.belongings')} span="two">{patient.belongings}</Field>
          <Field label={t('patient.extraInfo')} span="two">{patient.extraInfo}</Field>
        </dl>
      </section>

      <ReferralPanel patientId={patient.id} canManage={isAdmin} />

      <VisitorsPanel patientId={patient.id} canManage={isAdmin} />

      <ResidentReportsPanel patientId={patient.id} active={patient.active} />

      {/* Lo destructivo va al final, en su propia sección, con la consecuencia dicha en palabras */}
      {!patient.active && isAdmin && (
        <section className="danger-zone" aria-labelledby="danger-title">
          <h2 id="danger-title">{t('detail.deleteTitle')}</h2>
          <p>{t('detail.deleteText')}</p>
          <Button kind="danger--tertiary" onClick={() => setDialog('delete')}>
            {t('detail.delete')}
          </Button>
        </section>
      )}

      <ConfirmDialog
        open={dialog === 'discharge'}
        danger
        title={t('discharge.title', { name: fullName })}
        confirmLabel={t('discharge.confirm')}
        busy={busy}
        error={actionError}
        onClose={closeDialog}
        onConfirm={() =>
          run(async () => {
            setPatient(await dischargePatient(patient.id, exitDate))
            closeDialog()
            setFlash('discharged')
            active.reload()
          })
        }
      >
        <p>{t('discharge.body', { number: patient.number })}</p>
        <TextInput
          id="discharge-date"
          type="date"
          labelText={t('discharge.date')}
          value={exitDate}
          min={patient.entryDate}
          max={todayIso()}
          onChange={(e) => setExitDate(e.target.value)}
        />
      </ConfirmDialog>

      <ConfirmDialog
        open={dialog === 'delete'}
        danger
        title={t('deletePatient.title', { name: fullName })}
        confirmLabel={t('deletePatient.confirm')}
        busy={busy}
        error={actionError}
        onClose={closeDialog}
        onConfirm={() =>
          run(async () => {
            await deletePatient(patient.id)
            navigate('/antiguos', { replace: true })
          })
        }
      >
        <p>{t('deletePatient.body', { name: fullName, dni: patient.dni })}</p>
      </ConfirmDialog>

      {dialog === 'readmit' && (
        <ReadmitDialog
          patient={patient}
          nextNumber={nextNumber}
          onClose={closeDialog}
          onDone={(readmitted) => {
            setPatient(readmitted)
            closeDialog()
            setFlash('readmitted')
            active.reload()
          }}
        />
      )}
    </>
  )
}
