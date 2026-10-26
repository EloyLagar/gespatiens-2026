import { Button } from '@carbon/react'
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'
import { markMyTutorialSeen } from '../../api/users'
import { useAuth } from '../../auth/AuthContext'
import { TUTORIAL_STEPS } from './steps'

interface TutorialState {
  active: boolean
  start: () => void
}

const TutorialContext = createContext<TutorialState | null>(null)

// El tutorial de bienvenida: sale solo la primera vez que entra cada empleado (se guarda en su perfil,
// así no le vuelve a salir en otro equipo), y se repite desde Mi perfil o abriendo cualquier pantalla con ?tutorial. Es una tarjeta y no un diálogo modal: el
// tutorial va abriendo cada apartado y la pantalla de detrás tiene que verse
export function TutorialProvider({ children }: { children: ReactNode }) {
  const { user, replaceUser } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [index, setIndex] = useState<number | null>(() =>
    user && (!user.tutorialSeen || new URLSearchParams(location.search).has('tutorial')) ? 0 : null,
  )
  // Si se salta, se vuelve a la pantalla en la que estaba
  const returnTo = useRef(location.pathname)

  const steps = user ? TUTORIAL_STEPS[user.speciality] : []
  const step = index === null ? null : steps[index]

  // Se navega al cambiar de paso, no en un efecto: navigate cambia con cada URL y un efecto que dependiera
  // de él volvía a abrir el apartado tras cada redirección (/diario lleva a /diario/<hoy>), sin parar
  function goTo(next: number) {
    setIndex(next)
    const to = steps[next].to
    if (to) navigate(to)
  }

  const start = useCallback(() => {
    returnTo.current = location.pathname
    setIndex(0)
  }, [location.pathname])

  const close = useCallback(
    (finished: boolean) => {
      // Se da por visto ya en la sesión, sin esperar a la API. Si falla, volverá a salir la próxima vez que
      // entre: mejor eso que bloquear al empleado aquí con un error
      if (user && !user.tutorialSeen) {
        replaceUser({ ...user, tutorialSeen: true })
        markMyTutorialSeen().catch(() => {})
      }
      setIndex(null)
      // Al acabar, a la portada; al saltarlo, donde estaba (sin el ?tutorial)
      navigate(finished ? '/avisos' : returnTo.current, { replace: !finished })
    },
    [user, replaceUser, navigate],
  )

  const value = useMemo(() => ({ active: index !== null, start }), [index, start])

  return (
    <TutorialContext.Provider value={value}>
      {children}
      {user && step && index !== null && (
        <TutorialCard
          speciality={user.speciality}
          firstName={user.name.split(' ')[0]}
          stepKey={step.key}
          index={index}
          total={steps.length}
          onPrevious={() => goTo(index - 1)}
          onNext={() => (index === steps.length - 1 ? close(true) : goTo(index + 1))}
          onSkip={() => close(false)}
        />
      )}
    </TutorialContext.Provider>
  )
}

export function useTutorial(): TutorialState {
  const tutorial = useContext(TutorialContext)
  if (!tutorial) throw new Error('useTutorial debe usarse dentro de <TutorialProvider>')
  return tutorial
}

interface TutorialCardProps {
  speciality: string
  firstName: string
  stepKey: string
  index: number
  total: number
  onPrevious: () => void
  onNext: () => void
  onSkip: () => void
}

function TutorialCard({ speciality, firstName, stepKey, index, total, onPrevious, onNext, onSkip }: TutorialCardProps) {
  const { t } = useTranslation()
  const heading = useRef<HTMLHeadingElement>(null)
  const last = index === total - 1
  // El texto propio de la especialidad o, si no lo tiene, el común
  const text = (part: 'title' | 'text') =>
    t([`tutorial.${speciality}.${stepKey}.${part}`, `tutorial.common.${stepKey}.${part}`], { name: firstName })

  // Cada paso lleva el foco a su título: el lector de pantalla lo lee y el teclado sigue en la tarjeta
  useEffect(() => heading.current?.focus(), [index])

  function onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Escape') onSkip()
  }

  return (
    <section
      className="tour"
      role="dialog"
      aria-modal="false"
      aria-labelledby="tour-title"
      aria-describedby="tour-text"
      onKeyDown={onKeyDown}
    >
      <p className="tour__progress">{t('tutorial.progress', { current: index + 1, total })}</p>
      <h2 id="tour-title" ref={heading} tabIndex={-1}>
        {text('title')}
      </h2>
      <p id="tour-text" className="tour__text">
        {text('text')}
      </p>
      <div className="tour__bar" aria-hidden="true">
        <span style={{ width: `${((index + 1) / total) * 100}%` }} />
      </div>
      <div className="tour__actions">
        {!last && (
          <button type="button" className="text-action tour__skip" onClick={onSkip}>
            {t('tutorial.skip')}
          </button>
        )}
        <span className="tour__spacer" />
        {index > 0 && (
          <Button kind="secondary" size="sm" onClick={onPrevious}>
            {t('tutorial.previous')}
          </Button>
        )}
        <Button kind="primary" size="sm" onClick={onNext}>
          {index === 0 ? t('tutorial.begin') : last ? t('tutorial.finish') : t('tutorial.next')}
        </Button>
      </div>
    </section>
  )
}
