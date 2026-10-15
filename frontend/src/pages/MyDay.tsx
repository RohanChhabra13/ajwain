import { useEffect, useState } from 'react'
import { api, errorMessage, today } from '../api'
import type { MyDay as MyDayData, Task } from '../types'
import { useApp } from '../auth'
import TaskCard from '../components/TaskCard'
import TaskModal from '../components/TaskModal'

export default function MyDay() {
  const { workspaceId } = useApp()
  const [d, setD] = useState<MyDayData | null>(null)
  const [open, setOpen] = useState<number | null>(null)
  const [error, setError] = useState('')

  const load = () => api.get<MyDayData>(`/workspaces/${workspaceId}/my-day`).then(setD).catch((e) => setError(errorMessage(e)))
  useEffect(() => { setD(null); load() }, [workspaceId])

  const carry = async () => { await api.post(`/workspaces/${workspaceId}/my-day/carry-forward`); load() }
  const plan = async (t: Task) => { await api.patch(`/tasks/${t.id}`, { plannedDate: today() }); load() }
  const unplan = async (t: Task) => { await api.patch(`/tasks/${t.id}`, { clearPlannedDate: true }); load() }

  if (error) return <div className="error">{error}</div>
  if (!d) return <p className="muted">Loading…</p>
  const doneCount = d.planned.filter((t) => t.category === 'DONE').length

  return (
    <>
      <h1>My day</h1>
      <p className="muted">{new Date(d.date + 'T00:00').toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}{d.planned.length > 0 && ` · ${doneCount} of ${d.planned.length} done`}</p>

      {d.carryOver.length > 0 && (
        <div className="card notice row between">
          <span>{d.carryOver.length} unfinished task{d.carryOver.length > 1 ? 's' : ''} from earlier days.</span>
          <button onClick={carry}>Carry forward to today</button>
        </div>
      )}

      <h2>Planned for today</h2>
      {d.planned.length === 0 && <p className="muted">Nothing planned yet. Add tasks from the suggestions below, or open any task and choose “Today” under Planned for.</p>}
      {d.planned.map((t) => <TaskCard key={t.id} task={t} showProject onClick={() => setOpen(t.id)} actions={<button className="link" onClick={() => unplan(t)}>Remove from today</button>} />)}

      {d.suggestions.length > 0 && (
        <>
          <h2>Suggestions <span className="muted small">due today or overdue, not yet planned</span></h2>
          {d.suggestions.map((t) => <TaskCard key={t.id} task={t} showProject onClick={() => setOpen(t.id)} actions={<button className="secondary" onClick={() => plan(t)}>Add to today</button>} />)}
        </>
      )}
      {open && <TaskModal taskId={open} onClose={() => setOpen(null)} onChanged={load} onOpenOther={setOpen} />}
    </>
  )
}
