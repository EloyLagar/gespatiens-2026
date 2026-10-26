import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import { applyRememberedTheme, applyTheme } from '../../theme'
import { ThemeSection } from './ThemeSection'

const auth = {
  user: { id: 'u-1', name: 'Ana', email: 'ana@vientos.es', phoneNumber: '600111222', speciality: 'PSYCHOLOGIST', theme: 'SYSTEM' },
  replaceUser: vi.fn(),
}
vi.mock('../../auth/AuthContext', () => ({ useAuth: () => auth }))
const changeMyTheme = vi.fn((theme: string) => Promise.resolve({ ...auth.user, theme }))
vi.mock('../../api/users', () => ({ changeMyTheme: (theme: string) => changeMyTheme(theme) }))

// El modo del equipo: jsdom no trae matchMedia
function systemPrefersDark(dark: boolean) {
  window.matchMedia = ((query: string) => ({
    matches: dark && query.includes('dark'),
    addEventListener: () => {},
    removeEventListener: () => {},
  })) as unknown as typeof window.matchMedia
}

const root = document.documentElement

describe('Tema', () => {
  beforeEach(() => systemPrefersDark(false))
  afterEach(() => {
    localStorage.clear()
    applyTheme('SYSTEM')
    vi.clearAllMocks()
  })

  it('por defecto sigue al equipo', () => {
    systemPrefersDark(true)
    applyRememberedTheme()
    expect(root.dataset.theme).toBe('dark')
    expect(root).toHaveClass('cds--g100')
  })

  it('el oscuro cambia al momento, se guarda en el perfil y se recuerda en el navegador', async () => {
    render(<ThemeSection />)
    expect(screen.getByRole('button', { name: 'Sistema' })).toHaveAttribute('aria-pressed', 'true')

    await userEvent.click(screen.getByRole('button', { name: 'Oscuro' }))

    expect(screen.getByRole('button', { name: 'Oscuro' })).toHaveAttribute('aria-pressed', 'true')
    expect(root.dataset.theme).toBe('dark')
    expect(changeMyTheme).toHaveBeenCalledWith('DARK')
    expect(auth.replaceUser).toHaveBeenCalledWith(expect.objectContaining({ theme: 'DARK' }))
    expect(localStorage.getItem('gespatiens.theme')).toBe('DARK')
  })

  it('el claro manda aunque el equipo esté en oscuro', async () => {
    systemPrefersDark(true)
    render(<ThemeSection />)
    await userEvent.click(screen.getByRole('button', { name: 'Claro' }))
    expect(root.dataset.theme).toBe('light')
    expect(root).not.toHaveClass('cds--g100')
  })
})
