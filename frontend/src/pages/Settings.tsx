import { useState } from 'react'
import { api, errorMessage } from '../api'
import type { Status, StatusCategory, User, Workspace } from '../types'
import { useApp, useUser } from '../auth'

type Msg = { type: 'ok' | 'error'; text: string } | null

function useAction(setMsg: (m: Msg) => void) {
  return async (fn: () => Promise<unknown>, okText?: string) => {
    try { await fn(); setMsg(okText ? { type: 'ok', text: okText } : null) } catch (e) { setMsg({ type: 'error', text: errorMessage(e) }) }
  }
}

export default function Settings() {
  const { workspace, workspaceId, isManager, meta, refreshMeta, loadWorkspaces, setWorkspaceId, setUser } = useApp()
  const user = useUser()
  const [msg, setMsg] = useState<Msg>(null)
  const run = useAction(setMsg)
  const [wsName, setWsName] = useState(workspace?.name ?? '')
  const [newWs, setNewWs] = useState('')
  const [newOrg, setNewOrg] = useState('')
  const [status, setStatus] = useState<{ name: string; category: StatusCategory; color: string }>({ name: '', category: 'IN_PROGRESS', color: '#3b5bdb' })
  const [prio, setPrio] = useState({ name: '', level: 5, color: '#868e96' })
  const [field, setField] = useState({ name: '', type: 'TEXT', options: '' })
  const [profile, setProfile] = useState(user.name)
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '' })
  const base = `/workspaces/${workspaceId}`
  const after = async () => { await refreshMeta() }

  const swap = (i: number, j: number) => run(async () => {
    const a: Status = meta.statuses[i], b: Status = meta.statuses[j]
    await api.put(`${base}/statuses/${a.id}`, { ...a, position: j })
    await api.put(`${base}/statuses/${b.id}`, { ...b, position: i }); await after()
  })

  if (!workspace) return null

  return (
    <>
      <h1>Settings</h1>
      {msg && <div className={msg.type === 'error' ? 'error' : 'ok'}>{msg.text}</div>}

      <section className="card">
        <h2>Your account</h2>
        <form className="row wrap" onSubmit={(e) => { e.preventDefault(); run(async () => setUser(await api.put<User>('/auth/me', { name: profile })), 'Name updated.') }}>
          <input value={profile} onChange={(e) => setProfile(e.target.value)} required /><button>Save name</button>
        </form>
        <form className="row wrap" onSubmit={(e) => { e.preventDefault(); run(async () => { await api.post('/auth/change-password', pw); setPw({ currentPassword: '', newPassword: '' }) }, 'Password changed.') }}>
          <input type="password" placeholder="Current password" value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} required />
          <input type="password" placeholder="New password (min 8)" minLength={8} value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} required />
          <button>Change password</button>
        </form>
      </section>

      <section className="card">
        <h2>Organizations &amp; workspaces</h2>
        <p className="muted small">An organization is your business or team. Workspaces split its work into areas (for example one per department or client).</p>
        <form className="row wrap" onSubmit={(e) => { e.preventDefault(); run(async () => { const w = await api.post<Workspace>('/organizations', { name: newOrg }); setNewOrg(''); await loadWorkspaces(); setWorkspaceId(w.id) }, 'Organization created.') }}>
          <input placeholder="Start a new organization" value={newOrg} onChange={(e) => setNewOrg(e.target.value)} required /><button>Create organization</button>
        </form>
        {isManager && (
          <>
            <form className="row wrap" onSubmit={(e) => { e.preventDefault(); run(async () => { await api.patch(base, { name: wsName }); await loadWorkspaces() }, 'Workspace renamed.') }}>
              <input value={wsName} onChange={(e) => setWsName(e.target.value)} required /><button className="secondary">Rename this workspace</button>
            </form>
            <form className="row wrap" onSubmit={(e) => { e.preventDefault(); run(async () => { const w = await api.post<Workspace>('/workspaces', { organizationId: workspace.organizationId, name: newWs }); setNewWs(''); await loadWorkspaces(); setWorkspaceId(w.id) }, 'Workspace created.') }}>
              <input placeholder={`New workspace in ${workspace.organizationName}`} value={newWs} onChange={(e) => setNewWs(e.target.value)} required /><button className="secondary">Add workspace</button>
            </form>
          </>
        )}
      </section>

      {!isManager && <p className="muted">Workflow settings (statuses, priorities, custom fields) can be changed by owners and admins.</p>}
      {isManager && (
        <>
          <section className="card">
            <h2>Statuses</h2>
            <p className="muted small">The columns of your board. The category tells Workboard which ones mean “not started”, “in progress” and “done” (used for overdue, reports and recurring tasks).</p>
            {meta.statuses.map((s, i) => (
              <div key={s.id} className="row tight wrap">
                <span className="dotc" style={{ background: s.color ?? undefined }} />
                <input defaultValue={s.name} key={s.id + s.name} onBlur={(e) => e.target.value.trim() && e.target.value !== s.name && run(async () => { await api.put(`${base}/statuses/${s.id}`, { ...s, name: e.target.value }); await after() })} />
                <select value={s.category} onChange={(e) => run(async () => { await api.put(`${base}/statuses/${s.id}`, { ...s, category: e.target.value }); await after() })}>
                  <option value="TODO">Not started</option><option value="IN_PROGRESS">In progress</option><option value="DONE">Done</option>
                </select>
                <input type="color" value={s.color || '#868e96'} onChange={(e) => run(async () => { await api.put(`${base}/statuses/${s.id}`, { ...s, color: e.target.value }); await after() })} aria-label="Colour" />
                <button className="link" disabled={i === 0} onClick={() => swap(i, i - 1)} aria-label="Move up">↑</button>
                <button className="link" disabled={i === meta.statuses.length - 1} onClick={() => swap(i, i + 1)} aria-label="Move down">↓</button>
                <button className="link" onClick={() => run(async () => { await api.del(`${base}/statuses/${s.id}`); await after() })}>Delete</button>
              </div>
            ))}
            <form className="row wrap" onSubmit={(e) => { e.preventDefault(); run(async () => { await api.post(`${base}/statuses`, status); setStatus({ ...status, name: '' }); await after() }) }}>
              <input placeholder="New status" value={status.name} onChange={(e) => setStatus({ ...status, name: e.target.value })} required />
              <select value={status.category} onChange={(e) => setStatus({ ...status, category: e.target.value as StatusCategory })}><option value="TODO">Not started</option><option value="IN_PROGRESS">In progress</option><option value="DONE">Done</option></select>
              <button className="secondary">Add status</button>
            </form>
          </section>

          <section className="card">
            <h2>Priorities</h2>
            {meta.priorities.map((p) => (
              <div key={p.id} className="row tight wrap">
                <span className="dotc" style={{ background: p.color ?? undefined }} />
                <input defaultValue={p.name} key={p.id + p.name} onBlur={(e) => e.target.value.trim() && e.target.value !== p.name && run(async () => { await api.put(`${base}/priorities/${p.id}`, { ...p, name: e.target.value }); await after() })} />
                <label className="inline">Level <input type="number" style={{ width: 70 }} defaultValue={p.level} onBlur={(e) => Number(e.target.value) !== p.level && run(async () => { await api.put(`${base}/priorities/${p.id}`, { ...p, level: Number(e.target.value) }); await after() })} /></label>
                <button className="link" onClick={() => run(async () => { await api.del(`${base}/priorities/${p.id}`); await after() })}>Delete</button>
              </div>
            ))}
            <form className="row wrap" onSubmit={(e) => { e.preventDefault(); run(async () => { await api.post(`${base}/priorities`, prio); setPrio({ ...prio, name: '' }); await after() }) }}>
              <input placeholder="New priority" value={prio.name} onChange={(e) => setPrio({ ...prio, name: e.target.value })} required />
              <input type="number" style={{ width: 80 }} value={prio.level} onChange={(e) => setPrio({ ...prio, level: Number(e.target.value) })} title="Higher = more important" />
              <input type="color" value={prio.color} onChange={(e) => setPrio({ ...prio, color: e.target.value })} aria-label="Colour" />
              <button className="secondary">Add priority</button>
            </form>
          </section>

          <section className="card">
            <h2>Custom fields</h2>
            <p className="muted small">Extra information to track on every task, such as a client name, a budget, or a size.</p>
            {meta.customFields.map((f) => (
              <div key={f.id} className="row tight">
                <b>{f.name}</b><span className="muted small">{f.type.toLowerCase()}{f.options.length > 0 && `: ${f.options.join(', ')}`}</span>
                <button className="link" onClick={() => confirm(`Delete “${f.name}” and its values on all tasks?`) && run(async () => { await api.del(`${base}/custom-fields/${f.id}`); await after() })}>Delete</button>
              </div>
            ))}
            <form className="row wrap" onSubmit={(e) => { e.preventDefault(); run(async () => { await api.post(`${base}/custom-fields`, { name: field.name, type: field.type, options: field.type === 'SELECT' ? field.options.split(',') : [] }); setField({ name: '', type: 'TEXT', options: '' }); await after() }) }}>
              <input placeholder="Field name" value={field.name} onChange={(e) => setField({ ...field, name: e.target.value })} required />
              <select value={field.type} onChange={(e) => setField({ ...field, type: e.target.value })}><option value="TEXT">Text</option><option value="NUMBER">Number</option><option value="DATE">Date</option><option value="SELECT">Choice</option></select>
              {field.type === 'SELECT' && <input placeholder="Options, comma separated" value={field.options} onChange={(e) => setField({ ...field, options: e.target.value })} required />}
              <button className="secondary">Add field</button>
            </form>
          </section>
        </>
      )}
    </>
  )
}
