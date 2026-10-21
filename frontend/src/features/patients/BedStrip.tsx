import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import type { Bed } from './beds'

// El elemento propio de gespatiens: las 32 plazas numeradas del centro (RF10, RF11).
// Una plaza ocupada enlaza con la ficha de su residente; la siguiente libre se marca como la que
// recibirá la próxima alta. El color nunca es la única señal: cada plaza dice "libre" o el nombre
export function BedStrip({ beds, next }: { beds: Bed[]; next: number | null }) {
  const { t } = useTranslation()
  return (
    <ol className="beds" aria-label={t('beds.label')}>
      {beds.map(({ number, patient }) =>
        patient ? (
          <li key={number}>
            <Link
              to={`/residentes/${patient.id}`}
              className="bed bed--taken"
              aria-label={t('beds.taken', { number, name: `${patient.name} ${patient.surname}` })}
            >
              <span className="bed__number num">{number}</span>
              <span className="bed__name">{patient.name}</span>
            </Link>
          </li>
        ) : (
          <li key={number}>
            <span
              className={number === next ? 'bed bed--free bed--next' : 'bed bed--free'}
              aria-label={number === next ? t('beds.nextFree', { number }) : t('beds.free', { number })}
            >
              <span className="bed__number num">{number}</span>
              <span className="bed__name">{number === next ? t('beds.next') : t('beds.freeShort')}</span>
            </span>
          </li>
        ),
      )}
    </ol>
  )
}
