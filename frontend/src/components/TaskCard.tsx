import type { DragEventHandler, ReactNode } from 'react'
import type { Task } from '../types'

interface Props {
  task: Task
  onClick?: () => void
  draggable?: boolean
  onDragStart?: DragEventHandler<HTMLDivElement>
  showProject?: boolean
  actions?: ReactNode
}

export default function TaskCard({ task, onClick, draggable, onDragStart, showProject, actions }: Props) {
  return (
    <div className={`task ${task.overdue ? 'overdue' : ''} ${task.category === 'DONE' ? 'done' : ''}`} onClick={onClick} draggable={draggable} onDragStart={onDragStart}>
      <div className="task-title">{task.recurrence !== 'NONE' && <span title={`Repeats ${task.recurrence.toLowerCase()}`}>↻ </span>}{task.title}</div>
      <div className="task-meta">
        <span className="badge" style={{ background: (task.priorityColor ?? '#868e96') + '22', color: task.priorityColor ?? undefined }}>{task.priorityName}</span>
        {task.dueDate && <span className={task.overdue ? 'due late' : 'due'}>{task.dueDate}</span>}
        {task.subtaskTotal > 0 && <span className="muted small">☑ {task.subtaskDone}/{task.subtaskTotal}</span>}
        {task.epicName && <span className="muted small">◆ {task.epicName}</span>}
        {showProject && <span className="muted small">{task.projectName}</span>}
        {task.assignee && <span className="avatar" title={task.assignee.name}>{task.assignee.name[0].toUpperCase()}</span>}
      </div>
      {task.tags.length > 0 && <div className="tags">{task.tags.map((t) => <span key={t} className="tag">{t}</span>)}</div>}
      {actions && <div className="task-actions" onClick={(e) => e.stopPropagation()}>{actions}</div>}
    </div>
  )
}
