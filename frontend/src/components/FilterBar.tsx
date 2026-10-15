import type { ChangeEvent, ReactNode } from 'react'
import { useApp } from '../auth'

export interface Filter {
  q: string; statusId: string; priorityId: string; assignee: string; tag: string; overdue: boolean; dueFrom: string; dueTo: string
}

export const EMPTY_FILTER: Filter = { q: '', statusId: '', priorityId: '', assignee: '', tag: '', overdue: false, dueFrom: '', dueTo: '' }

interface Props { value: Filter; onChange: (f: Filter) => void; children?: ReactNode }

export default function FilterBar({ value, onChange, children }: Props) {
  const { meta } = useApp()
  const set = (k: keyof Filter) => (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    onChange({ ...value, [k]: e.target.type === 'checkbox' ? (e.target as HTMLInputElement).checked : e.target.value })
  const active = JSON.stringify(value) !== JSON.stringify(EMPTY_FILTER)
  return (
    <div className="filters">
      <input type="search" placeholder="Search tasks…" value={value.q} onChange={set('q')} />
      <select value={value.statusId} onChange={set('statusId')} aria-label="Status"><option value="">Any status</option>{meta.statuses.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</select>
      <select value={value.priorityId} onChange={set('priorityId')} aria-label="Priority"><option value="">Any priority</option>{meta.priorities.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select>
      <select value={value.assignee} onChange={set('assignee')} aria-label="Assignee">
        <option value="">Anyone</option><option value="me">Me</option><option value="none">Unassigned</option>
        {meta.members.map((m) => <option key={m.userId} value={m.userId}>{m.name}</option>)}
      </select>
      <select value={value.tag} onChange={set('tag')} aria-label="Tag"><option value="">Any tag</option>{meta.tags.map((t) => <option key={t}>{t}</option>)}</select>
      <label className="inline"><input type="checkbox" checked={value.overdue} onChange={set('overdue')} /> Overdue</label>
      <label className="inline">Due from <input type="date" value={value.dueFrom} onChange={set('dueFrom')} /></label>
      <label className="inline">to <input type="date" value={value.dueTo} onChange={set('dueTo')} /></label>
      {active && <button className="link" onClick={() => onChange(EMPTY_FILTER)}>Clear</button>}
      {children}
    </div>
  )
}
