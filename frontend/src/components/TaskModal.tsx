import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { api, errorMessage, today } from '../api'
import type { Activity, Attachment, Comment, Epic, Recurrence, Subtask, Task, TaskPatch } from '../types'
import { useApp } from '../auth'

const fmtSize = (n: number): string => (n > 1048576 ? `${(n / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(n / 1024))} KB`)

interface Props {
  taskId: number
  onClose: () => void
  onChanged?: () => void
  onOpenOther?: (taskId: number) => void
}

export default function TaskModal({ taskId, onClose, onChanged, onOpenOther }: Props) {
  const { meta, refreshMeta } = useApp()
  const [task, setTask] = useState<Task | null>(null)
  const [epics, setEpics] = useState<Epic[]>([])
  const [comments, setComments] = useState<Comment[]>([])
  const [activity, setActivity] = useState<Activity[]>([])
  const [files, setFiles] = useState<Attachment[]>([])
  const [body, setBody] = useState('')
  const [subTitle, setSubTitle] = useState('')
  const [error, setError] = useState('')

  const loadSide = async () => {
    const [c, a, f] = await Promise.all([api.get<Comment[]>(`/tasks/${taskId}/comments`), api.get<Activity[]>(`/tasks/${taskId}/activity`), api.get<Attachment[]>(`/tasks/${taskId}/attachments`)])
    setComments(c); setActivity(a); setFiles(f)
  }
  const load = async () => {
    const t = await api.get<Task>(`/tasks/${taskId}`)
    setTask(t)
    api.get<Epic[]>(`/projects/${t.projectId}/epics`).then(setEpics).catch(() => {})
    await loadSide()
  }
  useEffect(() => { setTask(null); load().catch((e) => setError(errorMessage(e))) }, [taskId])

  const guard = <A extends unknown[]>(fn: (...a: A) => Promise<unknown>) => async (...a: A) => {
    try { setError(''); await fn(...a) } catch (e) { setError(errorMessage(e)) }
  }

  const patch = guard(async (changes: TaskPatch) => {
    setTask(await api.patch<Task>(`/tasks/${taskId}`, changes))
    onChanged?.(); refreshMeta(); setActivity(await api.get<Activity[]>(`/tasks/${taskId}/activity`))
  })
  const addComment = guard(async (e: FormEvent) => {
    e.preventDefault()
    if (!body.trim()) return
    await api.post(`/tasks/${taskId}/comments`, { body }); setBody(''); loadSide()
  })
  const addSubtask = guard(async (e: FormEvent) => {
    e.preventDefault()
    if (!subTitle.trim()) return
    await api.post(`/tasks/${taskId}/subtasks`, { title: subTitle }); setSubTitle(''); load(); onChanged?.()
  })
  const toggleSub = guard(async (s: Subtask) => {
    const target = s.category === 'DONE' ? meta.statuses.find((x) => x.category === 'TODO') : meta.statuses.find((x) => x.category === 'DONE')
    if (!target) return
    await api.patch(`/tasks/${s.id}`, { statusId: target.id }); load(); onChanged?.()
  })
  const upload = guard(async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (file) { await api.upload(`/tasks/${taskId}/attachments`, file); loadSide() }
  })
  const removeFile = guard(async (id: number) => { await api.del(`/attachments/${id}`); loadSide() })
  const remove = guard(async () => {
    if (!confirm('Delete this task and its subtasks?')) return
    await api.del(`/tasks/${taskId}`); onChanged?.(); onClose()
  })

  const setCustom = (fieldId: number, value: string) => patch({ customFields: { [fieldId]: value } })

  return (
    <div className="overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        {!task ? <p className="muted">{error || 'Loading…'}</p> : (
          <>
            <div className="modal-head">
              <input className="title-input" defaultValue={task.title} key={task.id + task.title} onBlur={(e) => e.target.value.trim() && e.target.value !== task.title && patch({ title: e.target.value })} />
              <button className="link" onClick={onClose} aria-label="Close">✕</button>
            </div>
            <div className="muted small">{task.projectName}{task.parentId != null && <> · subtask of <button className="link" onClick={() => onOpenOther?.(task.parentId as number)}>#{task.parentId}</button></>}</div>
            {error && <div className="error">{error}</div>}

            <div className="grid2">
              <label>Status
                <select value={task.statusId} onChange={(e) => patch({ statusId: Number(e.target.value) })}>
                  {meta.statuses.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </label>
              <label>Priority
                <select value={task.priorityId} onChange={(e) => patch({ priorityId: Number(e.target.value) })}>
                  {meta.priorities.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                </select>
              </label>
              <label>Assignee
                <select value={task.assignee?.id ?? ''} onChange={(e) => e.target.value ? patch({ assigneeId: Number(e.target.value) }) : patch({ clearAssignee: true })}>
                  <option value="">Unassigned</option>
                  {meta.members.map((m) => <option key={m.userId} value={m.userId}>{m.name}</option>)}
                </select>
              </label>
              <label>Due date
                <input type="date" value={task.dueDate ?? ''} onChange={(e) => e.target.value ? patch({ dueDate: e.target.value }) : patch({ clearDueDate: true })} />
              </label>
              <label>Planned for
                <span className="row tight">
                  <input type="date" value={task.plannedDate ?? ''} onChange={(e) => e.target.value ? patch({ plannedDate: e.target.value }) : patch({ clearPlannedDate: true })} />
                  <button className="secondary" onClick={() => patch({ plannedDate: today() })}>Today</button>
                </span>
              </label>
              {!task.parentId && (
                <label>Epic
                  <select value={task.epicId ?? ''} onChange={(e) => e.target.value ? patch({ epicId: Number(e.target.value) }) : patch({ clearEpic: true })}>
                    <option value="">No epic</option>
                    {epics.map((e) => <option key={e.id} value={e.id}>{e.name}</option>)}
                  </select>
                </label>
              )}
              {!task.parentId && (
                <label>Repeats
                  <span className="row tight">
                    <select value={task.recurrence} onChange={(e) => patch({ recurrence: e.target.value as Recurrence })}>
                      <option value="NONE">Never</option><option value="DAILY">Daily</option><option value="WEEKLY">Weekly</option><option value="MONTHLY">Monthly</option>
                    </select>
                    {task.recurrence !== 'NONE' && <input type="number" min="1" style={{ width: 70 }} defaultValue={task.recurrenceInterval} onBlur={(e) => patch({ recurrenceInterval: Number(e.target.value) || 1 })} title="Every N periods" />}
                  </span>
                </label>
              )}
              <label>Tags (comma separated)
                <input defaultValue={task.tags.join(', ')} key={task.tags.join()} onBlur={(e) => patch({ tags: e.target.value.split(',').map((s) => s.trim()).filter(Boolean) })} />
              </label>
              {meta.customFields.map((f) => (
                <label key={f.id}>{f.name}
                  {f.type === 'SELECT'
                    ? <select value={task.customFields[f.id] ?? ''} onChange={(e) => setCustom(f.id, e.target.value)}><option value="">—</option>{f.options.map((o) => <option key={o}>{o}</option>)}</select>
                    : <input type={f.type === 'NUMBER' ? 'number' : f.type === 'DATE' ? 'date' : 'text'} step="any" defaultValue={task.customFields[f.id] ?? ''} key={f.id + (task.customFields[f.id] ?? '')} onBlur={(e) => e.target.value !== (task.customFields[f.id] ?? '') && setCustom(f.id, e.target.value)} />}
                </label>
              ))}
            </div>

            <label>Description
              <textarea rows={4} defaultValue={task.description ?? ''} onBlur={(e) => e.target.value !== (task.description ?? '') && patch({ description: e.target.value })} />
            </label>

            {!task.parentId && (
              <>
                <h3>Subtasks {task.subtasks.length > 0 && <span className="muted small">{task.subtaskDone}/{task.subtaskTotal}</span>}</h3>
                {task.subtasks.map((s) => (
                  <div key={s.id} className="subtask">
                    <input type="checkbox" checked={s.category === 'DONE'} onChange={() => toggleSub(s)} />
                    <button className="link grow left" onClick={() => onOpenOther?.(s.id)}>{s.title}</button>
                    {s.assignee && <span className="muted small">{s.assignee.name}</span>}
                  </div>
                ))}
                <form onSubmit={addSubtask} className="row"><input placeholder="Add a subtask…" value={subTitle} onChange={(e) => setSubTitle(e.target.value)} /><button>Add</button></form>
              </>
            )}

            <h3>Attachments</h3>
            {files.map((f) => (
              <div key={f.id} className="subtask">
                <button className="link grow left" onClick={guard(() => api.download(`/attachments/${f.id}/download`, f.fileName))}>📎 {f.fileName}</button>
                <span className="muted small">{fmtSize(f.size)} · {f.uploadedBy.name}</span>
                <button className="link" onClick={() => removeFile(f.id)} aria-label="Remove attachment">✕</button>
              </div>
            ))}
            <input type="file" onChange={upload} />
            <div className="muted small">Max 10 MB per file.</div>

            <h3>Comments</h3>
            {comments.map((c) => (
              <div key={c.id} className="comment"><b>{c.author.name}</b> <span className="muted small">{new Date(c.createdAt).toLocaleString()}</span><div>{c.body}</div></div>
            ))}
            <form onSubmit={addComment} className="row"><input placeholder="Write a comment…" value={body} onChange={(e) => setBody(e.target.value)} /><button>Send</button></form>

            <h3>Activity</h3>
            <ul className="activity">
              {activity.map((a) => <li key={a.id}><b>{a.actor.name}</b> {a.message} <span className="muted small">{new Date(a.createdAt).toLocaleString()}</span></li>)}
            </ul>
            <button className="danger" onClick={remove}>Delete task</button>
          </>
        )}
      </div>
    </div>
  )
}
