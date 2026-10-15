import { useEffect, useState } from 'react'
import { api, errorMessage, qs } from '../api'
import type { Report } from '../types'
import { useApp } from '../auth'

const iso = (d: Date): string => d.toLocaleDateString('en-CA')

export default function Reports() {
  const { workspaceId } = useApp()
  const end = new Date(), start = new Date(); start.setDate(end.getDate() - 29)
  const [range, setRange] = useState({ from: iso(start), to: iso(end) })
  const [r, setR] = useState<Report | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    setError('')
    api.get<Report>(`/workspaces/${workspaceId}/reports${qs(range)}`).then(setR).catch((e) => setError(errorMessage(e)))
  }, [workspaceId, range])

  const max = r ? Math.max(1, ...r.daily.map((d) => Math.max(d.created, d.completed))) : 1
  return (
    <>
      <div className="row between wrap"><h1>Reports</h1>
        <div className="row tight">
          <label className="inline">From <input type="date" value={range.from} onChange={(e) => setRange({ ...range, from: e.target.value })} /></label>
          <label className="inline">to <input type="date" value={range.to} onChange={(e) => setRange({ ...range, to: e.target.value })} /></label>
          <button className="secondary" onClick={() => api.download(`/workspaces/${workspaceId}/tasks/export.csv`, 'tasks.csv')}>Export tasks (CSV)</button>
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      {r && (
        <>
          <div className="stats">
            <div className="card stat"><div className="num">{r.created}</div><div className="muted">Created</div></div>
            <div className="card stat"><div className="num">{r.completed}</div><div className="muted">Completed</div></div>
            <div className="card stat"><div className={`num ${r.openOverdue ? 'late' : ''}`}>{r.openOverdue}</div><div className="muted">Open &amp; overdue</div></div>
            <div className="card stat"><div className="num">{r.avgCycleDays ?? '—'}</div><div className="muted">Avg days to complete</div></div>
          </div>

          <section className="card">
            <h2>Created vs completed per day</h2>
            <div className="chart" role="img" aria-label="Bar chart of tasks created and completed per day">
              {r.daily.map((d) => (
                <div key={d.date} className="chart-col" title={`${d.date}: ${d.created} created, ${d.completed} completed`}>
                  <div className="bars"><div className="b created" style={{ height: `${(d.created / max) * 100}%` }} /><div className="b completed" style={{ height: `${(d.completed / max) * 100}%` }} /></div>
                </div>
              ))}
            </div>
            <div className="muted small"><span className="dotc created" />Created <span className="dotc completed" style={{ marginLeft: 12 }} />Completed</div>
          </section>

          <div className="cols">
            <section className="card">
              <h2>By person</h2>
              <table className="table plain"><thead><tr><th>Name</th><th>Assigned</th><th>Completed</th><th>Overdue</th></tr></thead>
                <tbody>{r.members.map((m) => <tr key={m.userId}><td>{m.name}</td><td>{m.assigned}</td><td>{m.completed}</td><td className={m.overdue ? 'late' : ''}>{m.overdue}</td></tr>)}
                  {r.members.length === 0 && <tr><td colSpan={4} className="muted">No assigned tasks.</td></tr>}</tbody></table>
            </section>
            <section className="card">
              <h2>By project</h2>
              {r.projects.map((p) => (
                <div key={p.projectId} className="progress-row">
                  <div className="small">{p.name} <span className="muted">{p.done} of {p.total} done</span></div>
                  <div className="bar"><div style={{ width: `${p.total ? (p.done / p.total) * 100 : 0}%` }} /></div>
                </div>
              ))}
              {r.projects.length === 0 && <p className="muted">No projects yet.</p>}
              <h2>Open work by priority</h2>
              {r.openByPriority.map((p) => <div key={p.name} className="small">{p.name}: <b>{p.count}</b></div>)}
              {r.openByPriority.length === 0 && <p className="muted">All clear.</p>}
            </section>
          </div>
        </>
      )}
    </>
  )
}
