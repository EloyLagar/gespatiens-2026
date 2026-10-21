import { useCallback, useEffect, useRef, useState } from 'react'
import { acquireLock, beingEditedBy, LOCK_RENEW_MS, releaseLock } from '../api/editLocks'

export type LockState =
  | { status: 'idle' }
  | { status: 'acquiring' }
  | { status: 'held' }
  // Otra persona lo está editando
  | { status: 'busy'; holderName: string | null }
  | { status: 'error'; error: unknown }

// RF8: pedir el bloqueo al empezar a editar algo, renovarlo mientras se edita y soltarlo al terminar, al
// salir de la pantalla o al cerrar la pestaña. Si la pestaña se cierra de golpe, el bloqueo caduca solo
export function useEditLock(resource: string) {
  const [state, setState] = useState<LockState>({ status: 'idle' })
  const held = useRef(false)

  const release = useCallback(
    (keepalive = false) => {
      if (!held.current) return
      held.current = false
      // Si no se puede soltar (sin red), caduca solo: no hace falta avisar
      releaseLock(resource, keepalive).catch(() => {})
    },
    [resource],
  )

  // Devuelve si se ha conseguido: quien llama solo abre el formulario en ese caso
  const acquire = useCallback(async () => {
    setState({ status: 'acquiring' })
    try {
      await acquireLock(resource)
      held.current = true
      setState({ status: 'held' })
      return true
    } catch (err) {
      const busy = beingEditedBy(err)
      setState(busy ? { status: 'busy', holderName: busy.name } : { status: 'error', error: err })
      return false
    }
  }, [resource])

  const stop = useCallback(() => {
    release()
    setState({ status: 'idle' })
  }, [release])

  // Renovación mientras se edita. Si al renovar resulta que lo tiene otra persona (se quedó sin red más de
  // 90 s y otro empezó), se avisa: guardar daría error igualmente
  useEffect(() => {
    if (state.status !== 'held') return
    const timer = window.setInterval(() => {
      acquireLock(resource).catch((err: unknown) => {
        const busy = beingEditedBy(err)
        if (busy) {
          held.current = false
          setState({ status: 'busy', holderName: busy.name })
        }
      })
    }, LOCK_RENEW_MS)
    return () => window.clearInterval(timer)
  }, [state.status, resource])

  // Al salir de la pantalla o cerrar la pestaña
  useEffect(() => {
    const onLeave = () => release(true)
    window.addEventListener('pagehide', onLeave)
    return () => {
      window.removeEventListener('pagehide', onLeave)
      release(true)
    }
  }, [release])

  return { state, acquire, stop }
}
