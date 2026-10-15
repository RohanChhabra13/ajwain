import { useEffect, useState } from 'react'
import { api, errorMessage, qs } from '../api'
import type { Task } from '../types'
import { useApp } from '../auth'
import FilterBar, { EMPTY_FILTER, type Filter } from '../components/FilterBar'
import TaskModal from '../components/TaskModal'

export default function Tasks() {
  const { workspaceId } = useApp()
  const [filter, setFilter] = useState<Filter>(EMPTY_FILTER)
  const [subtasks, setSubtasks] = useState(false)
  const [archived, setArchived] = useState(false)
  const [tasks, setTasks] = useState<Task[]>([])
  const [open, setOpen] = useState<number | null>(null)
  const [error, setError] = useState('')

  const load = () => api.get<Task[]>(`/workspaces/${workspaceId}/tasks${qs({ ...filter, includeSubtasks: subtasks, includeArchived: archived })}`).then(setTasks).catch((e) => setError(errorMessage(e)))
  useEffect(() => { const t = setTimeout(load, 250); return () => clearTimeout(t) }, [workspaceId, filter, subtasks, archived])

  return (
    <>
      <div className="row between"><h1>All tasks</h1>
        <button className="secondary" onClick={() => api.download(`/workspaces/${workspaceId}/tasks/export.csv${qs({ includeArchived: archived })}`, 'tasks.csv')}>Export CSV</button>
      </div>
      <FilterBar value={filter} onChange={setFilter}>
        <label className="inline"><input type="checkbox" checked={subtasks} onChange={(e) => setSubtasks(e.target.checked)} /> Subtasks</label>
        <label className="inline"><input type="checkbox" checked={archived} onChange={(e) => setArchived(e.target.checked)} /> Archived projects</label>
      </FilterBar>
      {error && <div className="error">{error}</div>}
      <div className="table-wrap">
        <table className="table">
          <thead><tr><th>Task</th><th>Project</th><th>Status</th><th>Priority</th><th>Assignee</th><th>Due</th></tr></thead>
          <tbody>
            {tasks.map((t) => (
              <tr key={t.id} onClick={() => setOpen(t.id)} className={t.overdue ? 'overdue' : ''}>
                <td>{t.title}</td><td>{t.projectName}</td>
                <td><span className="dotc" style={{ background: t.statusColor ?? undefined }} />{t.statusName}</td><td>{t.priorityName}</td>
                <td>{t.assignee?.name ?? '—'}</td><td>{t.dueDate ?? '—'}</td>
              </tr>
            ))}
            {tasks.length === 0 && <tr><td colSpan={6} className="muted">No tasks match.</td></tr>}
          </tbody>
        </table>
      </div>
      <p className="muted small">{tasks.length} task{tasks.length === 1 ? '' : 's'}</p>
      {open && <TaskModal taskId={open} onClose={() => setOpen(null)} onChanged={load} onOpenOther={setOpen} />}
    </>
  )
}
