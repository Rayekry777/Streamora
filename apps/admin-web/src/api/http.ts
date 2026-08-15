export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

interface ErrorEnvelope {
  error?: { code?: string; message?: string }
}

export async function adminApiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    ...init,
    credentials: 'include',
    headers: { Accept: 'application/json', ...init.headers },
  })
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as ErrorEnvelope
    throw new ApiError(
      response.status,
      body.error?.code ?? 'REQUEST_FAILED',
      body.error?.message ?? '管理端请求失败',
    )
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
