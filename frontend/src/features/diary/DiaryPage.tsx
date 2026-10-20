import { Button, TextInput } from '@carbon/react'
import { ChevronLeft, ChevronRight } from '@carbon/icons-react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { getDiaryDay, type DiaryDay, type Shift } from '../../api/diary'
import { listPatients } from '../../api/patients'
import { listUsers } from '../../api/users'
import { useAuth } from '../../auth/AuthContext'
import { todayIso } from '../../shared/dates'
import { ErrorNotice, LoadingRows, PageHeader } from '../../shared/ui'
import { useApi } from '../../shared/useApi'
import { ActivitiesSection } from './ActivitiesSection'
import { OutingsSection } from './OutingsSection'
import { ReductionsSection } from './ReductionsSection'
import { ShiftPanel } from './ShiftPanel'

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

function shiftDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number)
  return todayIso(new Date(y, m - 1, d + days))
}

// La memoria: "un apartado llamado Diario [...] se podrá seleccionar la fecha de la página del diario a la que
// se desea acceder". Una página por día, con su fecha en la URL (/diario/2026-10-20) para poder enlazarla
export function DiaryPage() {
  const { date } = useParams()
  if (!date || !ISO_DATE.test(date)) return <Navigate to={`/diario/${todayIso()}`} replace />
  return <DiaryDayPage key={date} date={date} />
}

function DiaryDayPage({ date }: { date: string }) {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { user } = useAuth()
  // Registran rebajas el equipo médico (memoria) y la administración (decisión del centro)
  const canRecordReductions = user?.speciality === 'MEDICAL_TEAM' || user?.speciality === 'ADMINISTRATOR'
  // Valoran las actividades (RF6) los educadores y la administración (decisión del centro)
  const canEvaluate = user?.speciality === 'SOCIAL_EDUCATOR' || user?.speciality === 'ADMINISTRATOR'

  const day = useApi(() => getDiaryDay(date), [date])
  const staff = useApi(listUsers, [])
  // Los residentes en el centro, para elegir a quién rebajar o quién sale
  const residents = useApi(() => listPatients('ACTIVE'), [])
  const [current, setCurrent] = useState<DiaryDay | null>(null)
  useEffect(() => setCurrent(day.data), [day.data])

  const [y, m, d] = date.split('-').map(Number)
  const longDate = new Intl.DateTimeFormat(i18n.language === 'en' ? 'en-GB' : 'es-ES', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date(y, m - 1, d))
  const isToday = date === todayIso()

  function replaceShift(shift: Shift) {
    if (current) setCurrent({ ...current, shifts: current.shifts.map((s) => (s.part === shift.part ? shift : s)) })
  }

  return (
    <>
      <PageHeader
        title={t('diary.title')}
        subtitle={<span className="capitalize-first">{isToday ? t('diary.today', { date: longDate }) : longDate}</span>}
        actions={
          <div className="day-switcher">
            <Button kind="tertiary" size="md" hasIconOnly autoAlign tooltipPosition="bottom" renderIcon={ChevronLeft} iconDescription={t('diary.previous')}
              onClick={() => navigate(`/diario/${shiftDays(date, -1)}`)} />
            <TextInput
              id="diary-date"
              type="date"
              size="md"
              labelText={t('diary.pickDate')}
              hideLabel
              value={date}
              onChange={(e) => e.target.value && navigate(`/diario/${e.target.value}`)}
            />
            <Button kind="tertiary" size="md" hasIconOnly autoAlign tooltipPosition="bottom" renderIcon={ChevronRight} iconDescription={t('diary.next')}
              onClick={() => navigate(`/diario/${shiftDays(date, 1)}`)} />
            {!isToday && (
              <Button kind="ghost" size="md" onClick={() => navigate(`/diario/${todayIso()}`)}>
                {t('diary.goToday')}
              </Button>
            )}
          </div>
        }
      />

      {day.error ? (
        <ErrorNotice error={day.error} onRetry={day.reload} />
      ) : !current ? (
        <LoadingRows rows={4} label={t('common.loading')} />
      ) : (
        <>
          <section aria-labelledby="shifts-title">
            <h2 id="shifts-title" className="section-title">{t('diary.shifts')}</h2>
            <div className="shifts">
              {current.shifts.map((shift) => (
                <ShiftPanel key={shift.part} date={date} shift={shift} staff={staff.data ?? []} onSaved={replaceShift} />
              ))}
            </div>
          </section>

          <ReductionsSection
            date={date}
            reductions={current.reductions}
            canEdit={canRecordReductions}
            residents={residents.data ?? []}
            onChanged={(reductions) => {
              setCurrent({ ...current, reductions })
              // Una rebaja cambia quién figura como "Rebajado" en las listas de actividades
              day.reload()
            }}
          />

          <ActivitiesSection
            date={date}
            activities={current.activities}
            canEvaluate={canEvaluate}
            onChanged={(activities) => setCurrent({ ...current, activities })}
          />

          <OutingsSection
            date={date}
            outings={current.outings}
            residents={residents.data ?? []}
            onChanged={(outings) => setCurrent({ ...current, outings })}
          />
        </>
      )}
    </>
  )
}

