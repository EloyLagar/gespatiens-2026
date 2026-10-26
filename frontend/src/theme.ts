import type { Theme } from './api/types'

// El tema claro u oscuro. Lo elige cada empleado en su perfil (se guarda allí, como el idioma); por defecto,
// el de su equipo. El último elegido se recuerda también en este navegador: así la pantalla de entrada y la
// recarga de la página salen ya con el tema bueno, sin un destello del claro mientras llega la sesión
const KEY = 'gespatiens.theme'
const DARK_QUERY = '(prefers-color-scheme: dark)'

let current: Theme = 'SYSTEM'
let stopFollowing = () => {}

function systemIsDark(): boolean {
  return typeof window.matchMedia === 'function' && window.matchMedia(DARK_QUERY).matches
}

function paint(dark: boolean) {
  const root = document.documentElement
  root.dataset.theme = dark ? 'dark' : 'light'
  // La zona g100 de Carbon: sus componentes (botones, campos, avisos) pasan a sus colores oscuros
  root.classList.toggle('cds--g100', dark)
}

export function applyTheme(theme: Theme) {
  current = theme
  try {
    localStorage.setItem(KEY, theme)
  } catch {
    // Almacenamiento bloqueado: solo se pierde recordarlo antes de entrar
  }
  stopFollowing()
  stopFollowing = () => {}
  if (theme !== 'SYSTEM') return paint(theme === 'DARK')
  paint(systemIsDark())
  // Con el del equipo, si el empleado lo cambia (o cambia solo al anochecer) la aplicación lo sigue
  if (typeof window.matchMedia !== 'function') return
  const query = window.matchMedia(DARK_QUERY)
  const follow = (event: MediaQueryListEvent) => paint(event.matches)
  query.addEventListener('change', follow)
  stopFollowing = () => query.removeEventListener('change', follow)
}

export function currentTheme(): Theme {
  return current
}

// Al arrancar, antes de pintar nada: el último tema elegido en este navegador
export function applyRememberedTheme() {
  let remembered: string | null = null
  try {
    remembered = localStorage.getItem(KEY)
  } catch {
    // Sin almacenamiento: el del equipo
  }
  applyTheme(remembered === 'LIGHT' || remembered === 'DARK' ? remembered : 'SYSTEM')
}
