const BASE: string = import.meta.env.VITE_API_URL || '/api'

export const getToken = (): string | null => localStorage.getItem('token')
export const setToken = (t: string | null): void => (t ? localStorage.setItem('token', t) : localStorage.removeItem('token'))
export const today = (): string => new Date().toLocaleDateString('en-CA') // YYYY-MM-DD in local time

async function send(method: string, path: string, body?: unknown, isForm = false): Promise<Response> {
  const headers: Record<string, string> = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body && !isForm) headers['Content-Type'] = 'application/json'
  const res = await fetch(BASE + path, { method, headers, body: body ? (isForm ? (body as FormData) : JSON.stringify(body)) : undefined })
  if (res.status === 401 && token) {
    setToken(null)
    window.location.href = '/login'
    throw new Error('Session expired')
  }
  return res
}

async function json<T>(method: string, path: string, body?: unknown): Promise<T> {
  const res = await send(method, path, body)
  if (res.status === 204) return null as T
  const data = await res.json().catch(() => null)
  if (!res.ok) throw new Error(data?.error || `Request failed (${res.status})`)
  return data as T
}

export const api = {
  get: <T = unknown>(p: string) => json<T>('GET', p),
  post: <T = unknown>(p: string, b?: unknown) => json<T>('POST', p, b ?? {}),
  put: <T = unknown>(p: string, b?: unknown) => json<T>('PUT', p, b),
  patch: <T = unknown>(p: string, b?: unknown) => json<T>('PATCH', p, b),
  del: <T = unknown>(p: string) => json<T>('DELETE', p),
  upload: async <T = unknown>(p: string, file: File): Promise<T> => {
    const form = new FormData()
    form.append('file', file)
    const res = await send('POST', p, form, true)
    const data = await res.json().catch(() => null)
    if (!res.ok) throw new Error(data?.error || `Upload failed (${res.status})`)
    return data as T
  },
  download: async (p: string, filename: string): Promise<void> => {
    const res = await send('GET', p)
    if (!res.ok) throw new Error(`Download failed (${res.status})`)
    const url = URL.createObjectURL(await res.blob())
    const a = document.createElement('a')
    a.href = url; a.download = filename
    document.body.appendChild(a); a.click(); a.remove()
    URL.revokeObjectURL(url)
  },
}

export const qs = (obj: Record<string, string | number | boolean | null | undefined>): string => {
  const p = new URLSearchParams()
  Object.entries(obj).forEach(([k, v]) => { if (v !== '' && v !== null && v !== undefined && v !== false) p.set(k, String(v)) })
  const s = p.toString()
  return s ? `?${s}` : ''
}

/** Turns an unknown thrown value into a message for the UI. */
export const errorMessage = (e: unknown): string => (e instanceof Error ? e.message : String(e))
