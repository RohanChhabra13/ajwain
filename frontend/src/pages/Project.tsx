import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { api, errorMessage, qs } from '../api'
import type { Epic, Project as ProjectData, Task } from '../types'
import { useApp } from '../auth'
import FilterBar, { EMPTY_FILTER, type Filter } from '../components/FilterBar'
import TaskCard from '../components/TaskCard'
import TaskModal from '../components/TaskModal'

export default function Project() {
  const { id } = useParams()
  const { workspaceId, meta } = useApp()
  const nav = useNavigate()
  const [params, setParams] = useSearchParams()
  const [project, setProject] = useState<ProjectData | null>(null)
  const [tasks, setTasks] = useState<Task[]>([])
  const [epics, setEpics] = useState<Epic[]>([])
  const [filter, setFilter] = useState<Filter>(EMPTY_FILTER)
  const [epicFilter, setEpicFilter] = useState('')
  const [view, setView] = useState('board')
  const [open, setOpen] = useState<number | null>(params.get('task') ? Number(params.get('task')) : null)
  const [title, setTitle] = useState('')
  const [epicName, setEpicName] = useState('')
  const [error, setError] = useState('')

  const loadTasks = () => api.get<Task[]>(`/workspaces/${workspaceId}/tasks${qs({ ...filter, projectId: id, epicId: epicFilter, includeArchived: true })}`).then(setTasks).catch((e) => setError(errorMessage(e)))
  const loadEpics = () => api.get<Epic[]>(`/projects/${id}/epics`).then(setEpics)
  const loadProject = () => api.get<ProjectData>(`/projects/${id}`).then(setProject).catch((e) => setError(errorMessage(e)))
  const reload = () => { loadTasks(); loadEpics() }

  useEffect(() => { loadProject(); loadEpics() }, [id])
  useEffect(() => { const t = setTimeout(loadTasks, 200); return () => clearTimeout(t) }, [id, filter, epicFilter])
  useEffect(() => { if (open && params.get('task')) setParams({}, { replace: true }) }, [])

  const addTask = async (e: FormEvent) => {
    e.preventDefault()
    if (!title.trim()) return
    try { await api.post(`/projects/${id}/tasks`, { title, epicId: epicFilter ? Number(epicFilter) : undefined }); setTitle(''); reload() } catch (err) { setError(errorMessage(err)) }
  }
  const move = async (taskId: number, statusId: number) => {
    const st = meta.statuses.find((s) => s.id === statusId)
    if (!st) return
    setTasks((ts) => ts.map((t) => (t.id === taskId ? { ...t, statusId, statusName: st.name, category: st.category } : t)))
    try { await api.patch(`/tasks/${taskId}`, { statusId }); reload() } catch (err) { setError(errorMessage(err)); reload() }
  }
  const toggleArchive = async () => {
    if (!project) return
    await api.post(`/projects/${id}/${project.status === 'ACTIVE' ? 'archive' : 'restore'}`)
    loadProject()
  }
  const rename = async () => {
    if (!project) return
    const name = prompt('Project name', project.name)
    if (name?.trim()) { await api.put(`/projects/${id}`, { name, description: project.description }); loadProject() }
  }
  const addEpic = async (e: FormEvent) => {
    e.preventDefault()
    if (!epicName.trim()) return
    await api.post(`/projects/${id}/epics`, { name: epicName }); setEpicName(''); loadEpics()
  }
  const editEpic = async (ep: Epic) => {
    const name = prompt('Epic name', ep.name)
    if (name?.trim()) { await api.put(`/epics/${ep.id}`, { name, description: ep.description, dueDate: ep.dueDate, status: ep.status }); loadEpics(); loadTasks() }
  }
  const toggleEpic = async (ep: Epic) => { await api.put(`/epics/${ep.id}`, { name: ep.name, description: ep.description, dueDate: ep.dueDate, status: ep.status === 'DONE' ? 'OPEN' : 'DONE' }); loadEpics() }
  const removeEpic = async (ep: Epic) => { if (confirm(`Delete epic “${ep.name}”? Its tasks are kept.`)) { await api.del(`/epics/${ep.id}`); loadEpics(); loadTasks() } }

  if (error && !project) return <div className="error">{error}</div>
  if (!project) return <p className="muted">Loading…</p>
  const byPriority = (a: Task, b: Task) => b.priorityLevel - a.priorityLevel

  return (
    <>
      <div className="row between wrap">
        <h1>{project.name} {project.status === 'ARCHIVED' && <span className="badge">archived</span>}</h1>
        <div className="row">
          <button className="secondary" onClick={rename}>Rename</button>
          <button className="secondary" onClick={toggleArchive}>{project.status === 'ACTIVE' ? 'Archive' : 'Restore'}</button>
          <button className="link" onClick={() => nav('/projects')}>← All projects</button>
        </div>
      </div>
      {error && <div className="error">{error}</div>}

      <div className="row between wrap">
        <form className="row grow" onSubmit={addTask}>
          <input placeholder="Add a task and press Enter" value={title} onChange={(e) => setTitle(e.target.value)} />
          <button>Add</button>
        </form>
        <div className="tabs">{['board', 'list', 'backlog', 'epics'].map((v) => <button key={v} className={view === v ? 'active' : ''} onClick={() => setView(v)}>{v}</button>)}</div>
      </div>

      {view !== 'epics' && (
        <FilterBar value={filter} onChange={setFilter}>
          {epics.length > 0 && <select value={epicFilter} onChange={(e) => setEpicFilter(e.target.value)} aria-label="Epic"><option value="">Any epic</option>{epics.map((e) => <option key={e.id} value={e.id}>{e.name}</option>)}</select>}
        </FilterBar>
      )}

      {view === 'board' && (
        <div className="board" style={{ gridTemplateColumns: `repeat(${meta.statuses.length}, minmax(240px, 1fr))` }}>
          {meta.statuses.map((s) => {
            const col = tasks.filter((t) => t.statusId === s.id)
            return (
              <div key={s.id} className="column" onDragOver={(e) => e.preventDefault()}
                   onDrop={(e) => { const tid = Number(e.dataTransfer.getData('text/plain')); if (tid) move(tid, s.id) }}>
                <h3><span className="dotc" style={{ background: s.color ?? undefined }} />{s.name} <span className="muted">{col.length}</span></h3>
                {col.map((t) => (
                  <TaskCard key={t.id} task={t} draggable onDragStart={(e) => e.dataTransfer.setData('text/plain', String(t.id))} onClick={() => setOpen(t.id)}
                    actions={<select className="touch-only" value={t.statusId} onChange={(e) => move(t.id, Number(e.target.value))} aria-label="Move to">{meta.statuses.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}</select>} />
                ))}
              </div>
            )
          })}
        </div>
      )}

      {view === 'list' && (
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Task</th><th>Status</th><th>Priority</th><th>Assignee</th><th>Due</th></tr></thead>
            <tbody>
              {[...tasks].sort(byPriority).map((t) => (
                <tr key={t.id} onClick={() => setOpen(t.id)} className={t.overdue ? 'overdue' : ''}>
                  <td>{t.title}{t.subtaskTotal > 0 && <span className="muted small"> ☑ {t.subtaskDone}/{t.subtaskTotal}</span>}</td>
                  <td><span className="dotc" style={{ background: t.statusColor ?? undefined }} />{t.statusName}</td><td>{t.priorityName}</td>
                  <td>{t.assignee?.name ?? '—'}</td><td>{t.dueDate ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {view === 'backlog' && (
        <>
          <p className="muted small">Backlog: tasks that haven't been started, highest priority first.</p>
          {tasks.filter((t) => t.category === 'TODO').sort(byPriority).map((t) => <TaskCard key={t.id} task={t} onClick={() => setOpen(t.id)} />)}
        </>
      )}

      {view === 'epics' && (
        <>
          <form className="row" onSubmit={addEpic}><input placeholder="New epic (a larger goal that groups tasks)" value={epicName} onChange={(e) => setEpicName(e.target.value)} /><button>Add epic</button></form>
          {epics.length === 0 && <p className="muted">No epics yet.</p>}
          {epics.map((ep) => (
            <div key={ep.id} className="card epic">
              <div className="row between">
                <b>{ep.name} {ep.status === 'DONE' && <span className="badge">done</span>}</b>
                <span className="row tight">
                  <button className="link" onClick={() => { setEpicFilter(String(ep.id)); setView('board') }}>View tasks</button>
                  <button className="link" onClick={() => editEpic(ep)}>Rename</button>
                  <button className="link" onClick={() => toggleEpic(ep)}>{ep.status === 'DONE' ? 'Reopen' : 'Mark done'}</button>
                  <button className="link" onClick={() => removeEpic(ep)}>Delete</button>
                </span>
              </div>
              <div className="bar"><div style={{ width: `${ep.total ? (ep.done / ep.total) * 100 : 0}%` }} /></div>
              <div className="muted small">{ep.done} of {ep.total} tasks done</div>
            </div>
          ))}
        </>
      )}

      {open && <TaskModal taskId={open} onClose={() => setOpen(null)} onChanged={reload} onOpenOther={setOpen} />}
    </>
  )
}
