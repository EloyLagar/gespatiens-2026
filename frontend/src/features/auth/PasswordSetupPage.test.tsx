import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import '../../i18n'
import { PasswordSetupPage } from './PasswordSetupPage'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

// Lo que recibe el login al volver: el email para dejarlo puesto
function LoginProbe() {
  const location = useLocation()
  return <p>login {(location.state as { passwordCreatedFor?: string } | null)?.passwordCreatedFor}</p>
}

function renderAt(url: string) {
  render(
    <MemoryRouter initialEntries={[url]}>
      <Routes>
        <Route path="/crear-contrasena" element={<PasswordSetupPage />} />
        <Route path="/login" element={<LoginProbe />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('PasswordSetupPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  // Memoria final: el empleado nuevo crea su contraseña desde el enlace del correo, sin haber entrado
  it('saluda al empleado, crea la contraseña sin token de sesión y vuelve al login con su email', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(json({ name: 'Ana', email: 'ana@vientos.es' }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    renderAt('/crear-contrasena?token=abc123')

    expect(await screen.findByText(/Hola, Ana/)).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Contraseña nueva'), 'secreto123')
    await userEvent.type(screen.getByLabelText('Repite la contraseña nueva'), 'secreto123')
    await userEvent.click(screen.getByRole('button', { name: 'Crear contraseña' }))

    expect(await screen.findByText('login ana@vientos.es')).toBeInTheDocument()
    const [url, init] = fetchMock.mock.calls[1]
    expect(url).toBe('/api/auth/password-setup/abc123')
    expect(init.method).toBe('POST')
    expect(JSON.parse(init.body)).toEqual({ newPassword: 'secreto123' })
    expect(init.headers.Authorization).toBeUndefined()
  })

  it('explica qué hacer si el enlace ha caducado', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ detail: 'El enlace no es válido o ha caducado' }, 410)))
    renderAt('/crear-contrasena?token=viejo')

    expect(await screen.findByText('El enlace no sirve')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir a iniciar sesión' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Contraseña nueva')).not.toBeInTheDocument()
  })

  it('sin token no llama a la API', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    renderAt('/crear-contrasena')

    expect(await screen.findByText(/Pide a la administración un enlace nuevo/)).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
