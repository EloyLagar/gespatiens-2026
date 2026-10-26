import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import { applyRememberedTheme, applyTheme } from '../../theme'
import { ThemeSwitch } from './ThemeSwitch'

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

  it('cada pulsación pasa al siguiente: sistema, claro, oscuro y vuelta a empezar', async () => {
    systemPrefersDark(true)
    render(<ThemeSwitch />)
    const row = () => screen.getByRole('button', { name: /^Tema:/ })
    expect(row()).toHaveAccessibleName('Tema: del sistema')
    expect(row()).toHaveAttribute('title', 'Cambiar a: claro')

    // El claro manda aunque el equipo esté en oscuro
    await userEvent.click(row())
    expect(row()).toHaveAccessibleName('Tema: claro')
    expect(root.dataset.theme).toBe('light')
    expect(root).not.toHaveClass('cds--g100')

    await userEvent.click(row())
    expect(row()).toHaveAccessibleName('Tema: oscuro')
    expect(root.dataset.theme).toBe('dark')

    await userEvent.click(row())
    expect(row()).toHaveAccessibleName('Tema: del sistema')
  })

  it('cambia al momento, se guarda en el perfil y se recuerda en el navegador', async () => {
    render(<ThemeSwitch />)
    await userEvent.click(screen.getByRole('button', { name: 'Tema: del sistema' }))

    expect(changeMyTheme).toHaveBeenCalledWith('LIGHT')
    expect(auth.replaceUser).toHaveBeenCalledWith(expect.objectContaining({ theme: 'LIGHT' }))
    expect(localStorage.getItem('gespatiens.theme')).toBe('LIGHT')
  })
})
