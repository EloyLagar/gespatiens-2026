import { TextInput } from '@carbon/react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { readmitPatient } from '../../api/patients'
import type { Patient } from '../../api/types'
import { formatDate, todayIso } from '../../shared/dates'
import { ConfirmDialog } from '../../shared/ui'

interface Props {
  patient: Patient | null
  // El número que le tocará (RF11). null si el centro está lleno
  nextNumber: number | null
  onClose: () => void
  onDone: (patient: Patient) => void
}

export function ReadmitDialog({ patient, nextNumber, onClose, onDone }: Props) {
  const { t, i18n } = useTranslation()
  const [entryDate, setEntryDate] = useState(todayIso())
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)

  if (!patient) return null
  const fullName = `${patient.name} ${patient.surname}`

  async function confirm() {
    if (!patient) return
    setBusy(true)
    setError(null)
    try {
      onDone(await readmitPatient(patient.id, entryDate))
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <ConfirmDialog
      open
      title={t('readmit.title', { name: fullName })}
      confirmLabel={t('readmit.confirm')}
      busy={busy}
      error={error}
      onConfirm={confirm}
      onClose={onClose}
    >
      <p>
        {nextNumber === null
          ? t('readmit.full')
          : t('readmit.body', { number: nextNumber, exit: formatDate(patient.exitDate, i18n.language) })}
      </p>
      <TextInput
        id="readmit-date"
        type="date"
        labelText={t('readmit.date')}
        helperText={t('readmit.dateHelp')}
        value={entryDate}
        min={patient.exitDate ?? undefined}
        max={todayIso()}
        onChange={(e) => setEntryDate(e.target.value)}
      />
    </ConfirmDialog>
  )
}
