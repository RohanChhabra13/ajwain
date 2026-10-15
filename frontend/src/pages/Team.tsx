import { useEffect, useState, type FormEvent } from 'react'
import { api, errorMessage } from '../api'
import type { Invitation, InviteResult, Member, OrgRole } from '../types'
import { useApp, useUser } from '../auth'

export default function Team() {
  const { workspaceId, workspace, isManager, refreshMeta } = useApp()
  const user = useUser()
  const [members, setMembers] = useState<Member[]>([])
  const [invites, setInvites] = useState<Invitation[]>([])
  const [email, setEmail] = useState('')
  const [msg, setMsg] = useState<{ type: 'ok' | 'error'; text: string; link?: string } | null>(null)
  const isOwner = workspace?.myRole === 'OWNER'

  const load = async () => {
    setMembers(await api.get<Member[]>(`/workspaces/${workspaceId}/members`))
    if (isManager) setInvites(await api.get<Invitation[]>(`/workspaces/${workspaceId}/invitations`))
  }
  useEffect(() => { load() }, [workspaceId, isManager])

  const link = (token: string) => `${window.location.origin}/register?invite=${token}`
  const invite = async (e: FormEvent) => {
    e.preventDefault()
    try {
      const res = await api.post<InviteResult>(`/workspaces/${workspaceId}/invitations`, { email })
      setEmail('')
      setMsg(res.invited && res.invitation
        ? { type: 'ok', text: `${res.invitation.email} doesn't have an account yet. Send them this link:`, link: link(res.invitation.token) }
        : { type: 'ok', text: `${res.member?.name ?? 'They'} ${res.member ? 'was' : 'were'} added to the workspace.` })
      load(); refreshMeta()
    } catch (err) { setMsg({ type: 'error', text: errorMessage(err) }) }
  }
  const act = async (fn: () => Promise<unknown>) => { try { await fn(); load(); refreshMeta() } catch (err) { setMsg({ type: 'error', text: errorMessage(err) }) } }

  return (
    <>
      <h1>Team</h1>
      {isManager && (
        <form className="row" onSubmit={invite}>
          <input type="email" placeholder="Email address of a teammate" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <button>Add / invite</button>
        </form>
      )}
      {msg && <div className={msg.type === 'error' ? 'error' : 'ok'}>{msg.text}{msg.link && <div className="linkbox"><code>{msg.link}</code> <button className="secondary" onClick={() => navigator.clipboard?.writeText(msg.link ?? '')}>Copy</button></div>}</div>}

      <div className="table-wrap">
        <table className="table plain">
          <thead><tr><th>Name</th><th>Email</th><th>Role</th><th /></tr></thead>
          <tbody>
            {members.map((m) => (
              <tr key={m.userId}>
                <td>{m.name}{m.userId === user.id && <span className="muted"> (you)</span>}</td><td>{m.email}</td>
                <td>{isOwner && m.role !== 'OWNER'
                  ? <select value={m.role} onChange={(e) => act(() => api.patch(`/workspaces/${workspaceId}/members/${m.userId}`, { role: e.target.value as OrgRole }))}><option value="ADMIN">Admin</option><option value="MEMBER">Member</option></select>
                  : m.role.charAt(0) + m.role.slice(1).toLowerCase()}</td>
                <td>{isManager && m.role === 'MEMBER' && <button className="link" onClick={() => confirm(`Remove ${m.name} from this workspace?`) && act(() => api.del(`/workspaces/${workspaceId}/members/${m.userId}`))}>Remove</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="muted small">Owners and admins can see every workspace in the organization and manage settings. Members only see workspaces they've been added to.</p>

      {isManager && invites.length > 0 && (
        <>
          <h2>Pending invitations</h2>
          {invites.map((i) => (
            <div key={i.id} className="card row between wrap">
              <span>{i.email} <span className="muted small">expires {new Date(i.expiresAt).toLocaleDateString()}</span></span>
              <span className="row tight">
                <button className="secondary" onClick={() => navigator.clipboard?.writeText(link(i.token))}>Copy link</button>
                <button className="link" onClick={() => act(() => api.del(`/invitations/${i.id}`))}>Revoke</button>
              </span>
            </div>
          ))}
        </>
      )}
    </>
  )
}
