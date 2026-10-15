import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { Notification } from '../types'

export default function NotificationBell() {
  const [count, setCount] = useState(0)
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState<Notification[]>([])
  const nav = useNavigate()

  const poll = () => api.get<{ count: number }>('/notifications/unread-count').then((r) => setCount(r.count)).catch(() => {})
  useEffect(() => { poll(); const t = setInterval(poll, 30000); return () => clearInterval(t) }, [])

  const toggle = async () => {
    if (!open) setItems(await api.get<Notification[]>('/notifications'))
    setOpen(!open)
  }
  const go = async (n: Notification) => {
    if (!n.read) { await api.post(`/notifications/${n.id}/read`); poll() }
    setOpen(false)
    if (n.projectId) nav(`/projects/${n.projectId}?task=${n.taskId}`)
  }
  const readAll = async () => { await api.post('/notifications/read-all'); setItems(items.map((i) => ({ ...i, read: true }))); setCount(0) }

  return (
    <div className="bell">
      <button className="icon" onClick={toggle} aria-label="Notifications">🔔{count > 0 && <span className="dot">{count > 9 ? '9+' : count}</span>}</button>
      {open && (
        <div className="popover">
          <div className="row between"><b>Notifications</b><button className="link" onClick={readAll}>Mark all read</button></div>
          {items.length === 0 && <p className="muted small">Nothing yet.</p>}
          {items.map((n) => (
            <div key={n.id} className={`notif ${n.read ? '' : 'unread'}`} onClick={() => go(n)}>
              <div>{n.message}</div><div className="muted small">{new Date(n.createdAt).toLocaleString()}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
