export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

export interface ApiError {
  success: false
  message: string
  errorCode: string
  validationErrors: Record<string, string>
  timestamp: string
}

let csrfPromise: Promise<string> | null = null

export async function getCsrfToken(): Promise<string> {
  if (!csrfPromise) {
    csrfPromise = fetch('/api/v1/auth/csrf', { credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) throw new Error('Unable to initialize CSRF protection')
        const body = await response.json() as ApiResponse<{ token: string }>
        return body.data.token
      })
      .catch((error) => {
        csrfPromise = null
        throw error
      })
  }
  return csrfPromise
}

async function parseResponse<T>(response: Response): Promise<ApiResponse<T> | T> {
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    const error = body as ApiError
    throw new Error(error?.message || `Request failed with status ${response.status}`)
  }
  return body as ApiResponse<T>
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path, { credentials: 'include' })
  const body = await parseResponse<T>(response) as ApiResponse<T>
  return body.data
}

export async function apiMutation<T>(
  path: string,
  method: 'POST' | 'PUT' | 'PATCH' | 'DELETE',
  body?: unknown,
): Promise<T | null> {
  const token = await getCsrfToken()
  const response = await fetch(path, {
    method,
    credentials: 'include',
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      'X-CSRF-TOKEN': token,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (response.status === 204) return null
  const envelope = await parseResponse<T>(response) as ApiResponse<T>
  return envelope.data
}
