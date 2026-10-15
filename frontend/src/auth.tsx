import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, getToken, setToken } from './api'
import type { AuthResponse, Meta, User, Workspace } from './types'

interface AppState {
  user: User | null
  setUser: (u: User) => void
  loading: boolean
  workspaces: Workspace[]
  workspaceId: number | null
  setWorkspaceId: (id: number) => void
  loadWorkspaces: () => Promise<Workspace[]>
  meta: Meta
  refreshMeta: () => Promise<void>
  workspace: Workspace | undefined
  isManager: boolean
  login: (b: { email: string; password: string }) => Promise<void>
  register: (b: { name: string; email: string; password: string; organizationName?: string; inviteToken?: string }) => Promise<void>
  logout: () => void
}

const Ctx = createContext<AppState | null>(null)

export function useApp(): AppState {
  const v = useContext(Ctx)
  if (!v) throw new Error('useApp must be used inside AppProvider')
  return v
}

/** The signed-in user. Only call inside routes that are behind the login guard. */
export function useUser(): User {
  const { user } = useApp()
  if (!user) throw new Error('Not signed in')
  return user
}

const EMPTY_META: Meta = { statuses: [], priorities: [], members: [], customFields: [], tags: [] }

export function AppProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState<boolean>(!!getToken())
  const [workspaces, setWorkspaces] = useState<Workspace[]>([])
  const [workspaceId, setWorkspaceIdState] = useState<number | null>(() => Number(localStorage.getItem('workspaceId')) || null)
  const [meta, setMeta] = useState<Meta>(EMPTY_META)

  const loadWorkspaces = useCallback(async () => {
    const list = await api.get<Workspace[]>('/workspaces')
    setWorkspaces(list)
    setWorkspaceIdState((cur) => (list.some((w) => w.id === cur) ? cur : list[0]?.id ?? null))
    return list
  }, [])

  const refreshMeta = useCallback(async () => {
    if (!workspaceId) return setMeta(EMPTY_META)
    setMeta(await api.get<Meta>(`/workspaces/${workspaceId}/meta`))
  }, [workspaceId])

  useEffect(() => {
    if (!getToken()) return
    api.get<User>('/auth/me').then(setUser).then(loadWorkspaces).catch(() => setToken(null)).finally(() => setLoading(false))
  }, [loadWorkspaces])

  useEffect(() => { refreshMeta().catch(() => {}) }, [refreshMeta])

  const setWorkspaceId = (id: number) => { localStorage.setItem('workspaceId', String(id)); setWorkspaceIdState(id) }

  const authenticate = async (path: string, body: unknown) => {
    const res = await api.post<AuthResponse>(path, body)
    setToken(res.token)
    setUser(res.user)
    await loadWorkspaces()
  }
  const logout = () => { setToken(null); setUser(null); setWorkspaces([]); setMeta(EMPTY_META) }
  const workspace = workspaces.find((w) => w.id === workspaceId)

  return (
    <Ctx.Provider value={{
      user, setUser, loading, workspaces, workspaceId, setWorkspaceId, loadWorkspaces, meta, refreshMeta, workspace,
      isManager: workspace?.myRole === 'OWNER' || workspace?.myRole === 'ADMIN',
      login: (b) => authenticate('/auth/login', b),
      register: (b) => authenticate('/auth/register', b),
      logout,
    }}>
      {children}
    </Ctx.Provider>
  )
}
