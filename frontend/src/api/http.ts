// Cliente HTTP de la aplicación: el único sitio que hace fetch. Las pantallas llaman a api/patients.ts
// y api/users.ts y nunca ven URLs, cabeceras ni códigos de estado

// Error de la API con el formato ProblemDetail (RFC 9457) que devuelven los ExceptionHandler de Spring
export class ApiError extends Error {
  readonly status: number
  // El "detail" del ProblemDetail, en castellano. Vacío si la API no respondió o no lo trae
  readonly detail: string

  constructor(status: number, detail: string) {
    super(detail || `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.detail = detail
  }
}

type TokenProvider = () => string | null
type UnauthorizedHandler = () => void

let getToken: TokenProvider = () => null
let onUnauthorized: UnauthorizedHandler = () => {}

// AuthProvider registra aquí de dónde sale el token y qué hacer si la API lo rechaza
export function configureHttp(tokenProvider: TokenProvider, unauthorizedHandler: UnauthorizedHandler) {
  getToken = tokenProvider
  onUnauthorized = unauthorizedHandler
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  // El login es la única petición que no lleva token y cuyo 401 no cierra la sesión
  anonymous?: boolean
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const token = options.anonymous ? null : getToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    })
  } catch {
    // Sin respuesta: la API está parada o no hay red
    throw new ApiError(0, '')
  }

  if (response.status === 401 && !options.anonymous) {
    // Token caducado, manipulado o de un empleado borrado: hay que volver a entrar
    onUnauthorized()
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readDetail(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function readDetail(response: Response): Promise<string> {
  try {
    const problem = (await response.json()) as { detail?: unknown }
    return typeof problem.detail === 'string' ? problem.detail : ''
  } catch {
    return ''
  }
}
