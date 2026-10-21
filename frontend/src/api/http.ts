// Cliente HTTP de la aplicación: el único sitio que hace fetch. Las pantallas llaman a api/patients.ts
// y api/users.ts y nunca ven URLs, cabeceras ni códigos de estado

// Error de la API con el formato ProblemDetail (RFC 9457) que devuelven los ExceptionHandler de Spring
export class ApiError extends Error {
  readonly status: number
  // El "detail" del ProblemDetail, en castellano. Vacío si la API no respondió o no lo trae
  readonly detail: string
  // El ProblemDetail entero: algunos errores traen más datos (p. ej. lo que hay ahora en un apartado
  // de informe que otra persona ha modificado)
  readonly problem: Record<string, unknown>

  constructor(status: number, detail: string, problem: Record<string, unknown> = {}) {
    super(detail || `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.detail = detail
    this.problem = problem
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
  // Un fichero tal cual (la firma en PNG) en lugar de JSON
  file?: Blob
  // La petición sigue aunque se cierre la pestaña (soltar un bloqueo de edición al salir)
  keepalive?: boolean
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await send(path, options, 'application/json')
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

// Un fichero que da la API (los informes en PDF, la firma). No vale un enlace normal: la petición
// tiene que llevar el token
export async function download(path: string, accept = 'application/pdf'): Promise<{ fileName: string; blob: Blob }> {
  const response = await send(path, {}, accept)
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const fileName = /filename="?([^";]+)"?/.exec(disposition)?.[1] ?? 'documento.pdf'
  return { fileName, blob: await response.blob() }
}

async function send(path: string, options: RequestOptions, accept: string): Promise<Response> {
  const headers: Record<string, string> = { Accept: accept }
  if (options.file) {
    headers['Content-Type'] = options.file.type || 'application/octet-stream'
  } else if (options.body !== undefined) {
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
      body: options.file ?? (options.body !== undefined ? JSON.stringify(options.body) : undefined),
      keepalive: options.keepalive,
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
    const problem = await readProblem(response)
    throw new ApiError(response.status, typeof problem.detail === 'string' ? problem.detail : '', problem)
  }
  return response
}

async function readProblem(response: Response): Promise<Record<string, unknown>> {
  try {
    const problem: unknown = await response.json()
    return problem !== null && typeof problem === 'object' ? (problem as Record<string, unknown>) : {}
  } catch {
    return {}
  }
}
