import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, configureHttp, request } from './http'

function respond(status: number, body?: unknown) {
  return vi.fn().mockImplementation(async () =>
    new Response(body === undefined ? null : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('request', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    configureHttp(() => null, () => {})
  })

  it('envía el token como Bearer y devuelve el JSON', async () => {
    const fetchMock = respond(200, { ok: true })
    vi.stubGlobal('fetch', fetchMock)
    configureHttp(() => 'abc.def.ghi', () => {})

    expect(await request('/users/me')).toEqual({ ok: true })
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/users/me')
    expect(init.headers.Authorization).toBe('Bearer abc.def.ghi')
  })

  it('convierte un ProblemDetail en ApiError con su detalle', async () => {
    vi.stubGlobal('fetch', respond(409, { status: 409, detail: 'Ya existe un residente con el DNI 12345678Z' }))

    const error = await request('/patients', { method: 'POST', body: {} }).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(409)
    expect((error as ApiError).detail).toBe('Ya existe un residente con el DNI 12345678Z')
  })

  it('un 401 cierra la sesión', async () => {
    vi.stubGlobal('fetch', respond(401))
    const onUnauthorized = vi.fn()
    configureHttp(() => 'caducado', onUnauthorized)

    await expect(request('/patients')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('el login va sin token y su 401 no cierra ninguna sesión', async () => {
    const fetchMock = respond(401, { detail: 'Email o contraseña incorrectos' })
    vi.stubGlobal('fetch', fetchMock)
    const onUnauthorized = vi.fn()
    configureHttp(() => 'otro-token', onUnauthorized)

    await expect(request('/auth/login', { method: 'POST', body: {}, anonymous: true })).rejects.toBeInstanceOf(ApiError)
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBeUndefined()
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('sin respuesta del servidor lanza un ApiError con estado 0', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    const error = await request('/patients').catch((e: unknown) => e)
    expect((error as ApiError).status).toBe(0)
  })

  it('una respuesta 204 no intenta leer JSON', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation(async () => new Response(null, { status: 204 })))

    await expect(request('/patients/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})
