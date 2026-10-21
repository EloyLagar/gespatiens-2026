import { TextInput } from '@carbon/react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { admitPatient, listPatients } from '../../api/patients'
import { MAX_RESIDENTS } from '../../api/types'
import { todayIso } from '../../shared/dates'
import { PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { bedsOf, nextFreeNumber } from './beds'
import { emptyDetails, PatientForm } from './PatientForm'

export function AdmitPatientPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [entryDate, setEntryDate] = useState(todayIso())
  const active = useApi(() => listPatients('ACTIVE'), [])
  const next = useMemo(() => (active.data ? nextFreeNumber(bedsOf(active.data)) : null), [active.data])

  const subtitle = active.data
    ? next === null
      ? t('residents.subtitleFull', { max: MAX_RESIDENTS })
      : t('admit.subtitle', { number: next })
    : null

  return (
    <>
      <PageHeader
        title={t('admit.title')}
        subtitle={subtitle}
        breadcrumb={<Link to="/residentes">{t('nav.residents')}</Link>}
      />
      <PatientForm
        initial={emptyDetails()}
        submitLabel={t('admit.submit')}
        extraFields={
          <div className="form-grid form-grid--narrow">
            <TextInput
              id="patient-entryDate"
              type="date"
              labelText={t('patient.entryDate')}
              helperText={t('admit.entryDateHelp')}
              value={entryDate}
              max={todayIso()}
              onChange={(e) => setEntryDate(e.target.value)}
            />
          </div>
        }
        onSubmit={async (details) => {
          const patient = await admitPatient(details, entryDate || todayIso())
          navigate(`/residentes/${patient.id}`, { state: { flash: 'admitted' } })
        }}
        onCancel={() => navigate('/residentes')}
      />
    </>
  )
}
