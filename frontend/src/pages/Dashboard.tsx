import { useEffect, useState } from 'react'
import { api, errorMessage } from '../api'
import type { Dashboard as DashboardData } from '../types'
import { useApp } from '../auth'
import TaskCard from '../components/TaskCard'
import TaskModal from '../components/TaskModal'

export default function Dashboard() {
  const { workspaceId } = useApp()
  const [d, setD] = useState<DashboardData | null>(null)
  const [open, setOpen] = useState<number | null>(null)
  const [error, setError] = useState('')

  const load = () => api.get<DashboardData>(`/workspaces/${workspaceId}/dashboard`).then(setD).catch((e) => setError(errorMessage(e)))
  useEffect(() => { setD(null); load() }, [workspaceId])

  if (error) return <div className="error">{error}</div>
  if (!d) return <p className="muted">Loading…</p>

  return (
    <>
      <h1>Dashboard</h1>
      <div className="stats">
        {d.statusCounts.map((s) => (
          <div key={s.statusId} className="card stat"><div className="num">{s.count}</div><div className="muted"><span className="dotc" style={{ background: s.color ?? undefined }} />{s.name}</div></div>
        ))}
        <div className="card stat"><div className={`num ${d.overdue.length ? 'late' : ''}`}>{d.overdue.length}</div><div className="muted">Overdue</div></div>
      </div>

      <div className="cols">
        <section className="card">
          <h2>Overdue</h2>
          {d.overdue.length === 0 && <p className="muted">Nothing overdue.</p>}
          {d.overdue.map((t) => <TaskCard key={t.id} task={t} showProject onClick={() => setOpen(t.id)} />)}
        </section>
        <section className="card">
          <h2>Due today</h2>
          {d.dueToday.length === 0 && <p className="muted">Nothing due today.</p>}
          {d.dueToday.map((t) => <TaskCard key={t.id} task={t} showProject onClick={() => setOpen(t.id)} />)}
        </section>
      </div>

      <div className="cols">
        <section className="card">
          <h2>Team progress</h2>
          {d.teamProgress.length === 0 && <p className="muted">No assigned tasks yet.</p>}
          {d.teamProgress.map((m) => {
            const sum = m.todo + m.inProgress + m.done
            return (
              <div key={m.userId} className="progress-row">
                <div className="small">{m.name} <span className="muted">{m.done} of {sum} done · {m.inProgress} in progress</span></div>
                <div className="bar"><div style={{ width: `${sum ? (m.done / sum) * 100 : 0}%` }} /></div>
              </div>
            )
          })}
        </section>
        <section className="card">
          <h2>Recent activity</h2>
          <ul className="activity">{d.recentActivity.slice(0, 12).map((a) => <li key={a.id}><b>{a.actor.name}</b> {a.message}</li>)}</ul>
        </section>
      </div>
      {open && <TaskModal taskId={open} onClose={() => setOpen(null)} onChanged={load} onOpenOther={setOpen} />}
    </>
  )
}
