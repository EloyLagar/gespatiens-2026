import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { login as loginRequest } from '../api/auth'
import { configureHttp } from '../api/http'
import type { User } from '../api/types'
import { loadSession, saveSession, sessionFromLogin, type Session } from './session'

interface AuthState {
  user: User | null
  isAdmin: boolean
  // Por qué se cerró la sesión, para explicarlo en el login en vez de mandar ahí sin más
  endedBecause: 'expired' | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  // Tras editar el propio perfil, para que la barra lateral muestre el nombre nuevo
  replaceUser: (user: User) => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => loadSession())
  const [endedBecause, setEndedBecause] = useState<'expired' | null>(null)
  // fetch lee el token de aquí: así no hay que pasarlo por props a cada pantalla
  const tokenRef = useRef<string | null>(session?.token ?? null)
  const expireRef = useRef<() => void>(() => {})

  // Se configura durante el render y no en un useEffect: React ejecuta los efectos de los hijos antes
  // que los del padre, y las pantallas piden sus datos en un efecto. Configurado en un efecto, la
  // primera petición tras recargar la página salía sin token, recibía un 401 y cerraba la sesión
  configureHttp(
    () => tokenRef.current,
    () => expireRef.current(),
  )

  const store = useCallback((next: Session | null) => {
    tokenRef.current = next?.token ?? null
    saveSession(next)
    setSession(next)
  }, [])

  const logout = useCallback(() => store(null), [store])

  const expire = useCallback(() => {
    if (tokenRef.current) setEndedBecause('expired')
    store(null)
  }, [store])
  expireRef.current = expire

  // Cierra la sesión justo cuando caduca el token, aunque la pantalla esté quieta
  useEffect(() => {
    if (!session) return
    const ms = new Date(session.expiresAt).getTime() - Date.now()
    const timer = window.setTimeout(expire, Math.max(ms, 0))
    return () => window.clearTimeout(timer)
  }, [session, expire])

  const login = useCallback(
    async (email: string, password: string) => {
      const response = await loginRequest(email, password)
      setEndedBecause(null)
      store(sessionFromLogin(response))
    },
    [store],
  )

  const replaceUser = useCallback(
    (user: User) => {
      if (session) store({ ...session, user })
    },
    [session, store],
  )

  const value = useMemo<AuthState>(
    () => ({
      user: session?.user ?? null,
      isAdmin: session?.user.speciality === 'ADMINISTRATOR',
      endedBecause,
      login,
      logout,
      replaceUser,
    }),
    [session, endedBecause, login, logout, replaceUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const auth = useContext(AuthContext)
  if (!auth) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return auth
}
