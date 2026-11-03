import type { LoginResponse, User } from '../api/types'

// La sesión se guarda en sessionStorage: sobrevive a recargar la página pero se borra al cerrar
// la pestaña, y no se comparte con otras pestañas. En un ordenador compartido del centro es lo
// prudente. El token caduca solo a las 8 horas (gespatiens.jwt.expiration en Spring)
const KEY = 'gespatiens.session'

export interface Session {
  token: string
  expiresAt: string
  user: User
}

export function sessionFromLogin(response: LoginResponse): Session {
  return { token: response.accessToken, expiresAt: response.expiresAt, user: response.user }
}

export function isExpired(session: Session, now: Date = new Date()): boolean {
  return new Date(session.expiresAt).getTime() <= now.getTime()
}

export function loadSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) return null
    const session = JSON.parse(raw) as Session
    return isExpired(session) ? null : session
  } catch {
    return null
  }
}

export function saveSession(session: Session | null) {
  try {
    if (session) sessionStorage.setItem(KEY, JSON.stringify(session))
    else sessionStorage.removeItem(KEY)
  } catch {
    // Almacenamiento bloqueado (modo privado estricto): la sesión vive solo en memoria
  }
}

// Tras entrar se enseña una vez el acceso anterior (LastAccessNotice). La marca vive en sessionStorage, como la
// sesión: si se recarga la página antes de cerrar el aviso, sigue saliendo
const LAST_ACCESS_KEY = 'gespatiens.lastAccessPending'

export function setLastAccessPending(pending: boolean) {
  try {
    if (pending) sessionStorage.setItem(LAST_ACCESS_KEY, '1')
    else sessionStorage.removeItem(LAST_ACCESS_KEY)
  } catch {
    // Sin almacenamiento: el aviso no sale, no pasa nada
  }
}

export function isLastAccessPending(): boolean {
  try {
    return sessionStorage.getItem(LAST_ACCESS_KEY) === '1'
  } catch {
    return false
  }
}
