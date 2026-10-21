import { useCallback, useEffect, useState } from 'react'

interface ApiState<T> {
  data: T | null
  error: unknown
  loading: boolean
  reload: () => void
}

// Carga datos de la API al montar la pantalla. Un reload conserva los datos que ya había en pantalla
// mientras llegan los nuevos, para que la tabla no parpadee
export function useApi<T>(load: () => Promise<T>, deps: unknown[]): ApiState<T> {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [loading, setLoading] = useState(true)
  const [version, setVersion] = useState(0)

  const stableLoad = useCallback(load, deps)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    stableLoad()
      .then((result) => {
        if (cancelled) return
        setData(result)
        setError(null)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [stableLoad, version])

  const reload = useCallback(() => setVersion((v) => v + 1), [])
  return { data, error, loading, reload }
}
