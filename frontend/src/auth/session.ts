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
