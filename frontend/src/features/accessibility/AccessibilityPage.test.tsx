import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import '../../i18n'
import { AccessibilityPage } from './AccessibilityPage'

const auth = vi.hoisted(() => ({ user: null as null | { id: string } }))
vi.mock('../../auth/AuthContext', () => ({ useAuth: () => auth }))

describe('AccessibilityPage', () => {
  it('dice qué cumple, cómo se comprueba, sus limitaciones y dónde avisar, sin haber entrado', () => {
    render(<MemoryRouter><AccessibilityPage /></MemoryRouter>)

    expect(screen.getByRole('heading', { level: 1, name: 'Declaración de accesibilidad' })).toBeInTheDocument()
    expect(screen.getByText(/WCAG\) 2.1 en el nivel AA/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Limitaciones conocidas' })).toBeInTheDocument()
    expect(screen.getByText(/PDF no están etiquetados/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'abrir una incidencia' }))
      .toHaveAttribute('href', 'https://github.com/EloyLagar/gespatiens-2026/issues')
    expect(screen.getByRole('link', { name: 'Volver' })).toHaveAttribute('href', '/login')
  })
})
