import { render, screen, waitFor } from '@testing-library/react'
import { useEffect, useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '../api/http'
import { AuthProvider, useAuth } from './AuthContext'

const SESSION = {
  token: 'token-guardado',
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  user: { id: 'u1', name: 'Ana', email: 'ana@vientos.es', phoneNumber: '600111222', speciality: 'PSYCHOLOGIST' },
}

// Igual que las pantallas: pide datos en un useEffect nada más montarse
function ScreenThatLoadsOnMount() {
  const { user } = useAuth()
  const [status, setStatus] = useState('cargando')
  useEffect(() => {
    request('/patients').then(() => setStatus('cargado'), () => setStatus('error'))
  }, [])
  return (
    <p>
      {user ? user.name : 'sin sesión'} · {status}
    </p>
  )
}

describe('AuthProvider', () => {
  beforeEach(() => sessionStorage.setItem('gespatiens.session', JSON.stringify(SESSION)))
  afterEach(() => {
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  // Regresión: con el cliente HTTP configurado en un useEffect del provider, el efecto del hijo
  // corría antes, la primera petición tras recargar salía sin token, volvía 401 y cerraba la sesión
  it('la primera petición tras recargar la página ya lleva el token guardado', async () => {
    const fetchMock = vi.fn().mockImplementation(async (_url: string, init: RequestInit) => {
      const auth = (init.headers as Record<string, string>).Authorization
      return new Response('[]', { status: auth === 'Bearer token-guardado' ? 200 : 401 })
    })
    vi.stubGlobal('fetch', fetchMock)

    render(
      <AuthProvider>
        <ScreenThatLoadsOnMount />
      </AuthProvider>,
    )

    await waitFor(() => expect(screen.getByText('Ana · cargado')).toBeInTheDocument())
    expect(sessionStorage.getItem('gespatiens.session')).not.toBeNull()
  })

  it('un 401 de la API cierra la sesión', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation(async () => new Response(null, { status: 401 })))

    render(
      <AuthProvider>
        <ScreenThatLoadsOnMount />
      </AuthProvider>,
    )

    await waitFor(() => expect(screen.getByText('sin sesión · error')).toBeInTheDocument())
    expect(sessionStorage.getItem('gespatiens.session')).toBeNull()
  })

  it('una sesión caducada no se carga', () => {
    sessionStorage.setItem('gespatiens.session', JSON.stringify({ ...SESSION, expiresAt: '2020-01-01T00:00:00Z' }))
    vi.stubGlobal('fetch', vi.fn().mockImplementation(async () => new Response('[]')))

    render(
      <AuthProvider>
        <ScreenThatLoadsOnMount />
      </AuthProvider>,
    )

    expect(screen.getByText(/sin sesión/)).toBeInTheDocument()
  })
})
