import { Edit } from '@carbon/icons-react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { listLocks, type EditLock } from '../api/editLocks'
import { ErrorNotice } from './ui'
import type { LockState } from './useEditLock'

// Cada cuánto se mira quién está editando qué en una pantalla
const LOCKS_POLL_MS = 15_000

// RF8: lo que están editando otras personas dentro de algo (un informe, una página del diario), para avisar
// antes de que nadie lo intente. Cuando alguien termina, onChange permite recargar para ver lo que ha guardado
export function useOthersEditing(prefixes: string[], onChange: () => void): Map<string, EditLock> {
  const [others, setOthers] = useState<Map<string, EditLock>>(new Map())
  const changed = useRef(onChange)
  changed.current = onChange
  const key = prefixes.join('|')

  useEffect(() => {
    let cancelled = false
    let previous: string | null = null
    const poll = () =>
      Promise.all(key.split('|').map((prefix) => listLocks(prefix)))
        .then((lists) => {
          if (cancelled) return
          const theirs = lists.flat().filter((lock) => !lock.mine)
          const now = theirs.map((lock) => lock.resource).sort().join('|')
          if (previous !== null && now !== previous) changed.current()
          previous = now
          setOthers(new Map(theirs.map((lock) => [lock.resource, lock])))
        })
        // Sin red no se puede saber: se deja lo último que se supo y el servidor sigue protegiendo los datos
        .catch(() => {})
    poll()
    const timer = window.setInterval(poll, LOCKS_POLL_MS)
    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [key])

  return others
}

// "Marta lo está editando": lo ve quien no lo tiene abierto
export function EditingBadge({ name }: { name: string | null }) {
  const { t } = useTranslation()
  return (
    <span className="editing-badge">
      <Edit size={16} aria-hidden="true" />
      {t('editing.editing', { name: name ?? t('notices.formerEmployee') })}
    </span>
  )
}

// El aviso cuando no se ha podido empezar a editar
export function LockNotice({ state }: { state: LockState }) {
  const { t } = useTranslation()
  if (state.status === 'busy') {
    return (
      <p className="form-error" role="alert">
        {t('editing.busy', { name: state.holderName ?? t('notices.formerEmployee') })}
      </p>
    )
  }
  if (state.status === 'error') return <ErrorNotice error={state.error} />
  return null
}
