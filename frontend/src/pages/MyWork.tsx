import { useEffect, useState } from 'react'
import { api } from '../api'
import type { Task } from '../types'
import { useApp } from '../auth'
import TaskCard from '../components/TaskCard'
import TaskModal from '../components/TaskModal'

export default function MyWork() {
  const { workspaceId, meta } = useApp()
  const [tasks, setTasks] = useState<Task[]>([])
  const [open, setOpen] = useState<number | null>(null)
  const [showDone, setShowDone] = useState(false)

  const load = () => api.get<Task[]>(`/workspaces/${workspaceId}/my-work`).then(setTasks)
  useEffect(() => { load() }, [workspaceId])

  const visible = tasks.filter((t) => showDone || t.category !== 'DONE')
  return (
    <>
      <div className="row between"><h1>My work</h1>
        <label className="inline"><input type="checkbox" checked={showDone} onChange={(e) => setShowDone(e.target.checked)} /> Show completed</label>
      </div>
      {visible.length === 0 && <p className="muted">Nothing assigned to you. Open a project and assign yourself a task.</p>}
      {meta.statuses.map((s) => {
        const list = visible.filter((t) => t.statusId === s.id)
        return list.length > 0 && (
          <section key={s.id}>
            <h2><span className="dotc" style={{ background: s.color ?? undefined }} />{s.name}</h2>
            {list.map((t) => <TaskCard key={t.id} task={t} showProject onClick={() => setOpen(t.id)} />)}
          </section>
        )
      })}
      {open && <TaskModal taskId={open} onClose={() => setOpen(null)} onChanged={load} onOpenOther={setOpen} />}
    </>
  )
}
