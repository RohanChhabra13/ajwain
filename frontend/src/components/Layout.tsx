import { NavLink, Outlet } from 'react-router-dom'
import { useApp, useUser } from '../auth'
import NotificationBell from './NotificationBell'

const NAV: [string, string, boolean?][] = [
  ['/', 'Dashboard', true], ['/my-day', 'My day'], ['/my-work', 'My work'], ['/tasks', 'All tasks'],
  ['/projects', 'Projects'], ['/reports', 'Reports'], ['/team', 'Team'], ['/settings', 'Settings'],
]

export default function Layout() {
  const { logout, workspaces, workspaceId, setWorkspaceId, workspace } = useApp()
  const user = useUser()
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">Workboard</div>
        {workspaces.length > 1
          ? <select value={workspaceId ?? ''} onChange={(e) => setWorkspaceId(Number(e.target.value))} aria-label="Workspace">
              {workspaces.map((w) => <option key={w.id} value={w.id}>{w.organizationName} / {w.name}</option>)}
            </select>
          : <div className="muted small">{workspace?.organizationName}</div>}
        <nav>{NAV.map(([to, label, end]) => <NavLink key={to} to={to} end={end}>{label}</NavLink>)}</nav>
        <div className="sidebar-foot">
          <div className="row tight"><span className="avatar lg">{user.name[0].toUpperCase()}</span><span className="small">{user.name}</span><NotificationBell /></div>
          <button className="link" onClick={logout}>Log out</button>
        </div>
      </aside>
      <main className="content">{workspaceId ? <Outlet /> : <p className="muted">You don't belong to a workspace yet. Ask your team owner for an invite link.</p>}</main>
    </div>
  )
}
