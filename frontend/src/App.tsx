import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { useApp } from './auth'
import Layout from './components/Layout'
import Auth from './pages/Auth'
import AcceptInvite from './pages/AcceptInvite'
import Dashboard from './pages/Dashboard'
import MyDay from './pages/MyDay'
import MyWork from './pages/MyWork'
import Tasks from './pages/Tasks'
import Projects from './pages/Projects'
import Project from './pages/Project'
import Reports from './pages/Reports'
import Team from './pages/Team'
import Settings from './pages/Settings'

function Protected({ children }: { children: ReactNode }) {
  const { user, loading } = useApp()
  if (loading) return <div className="center muted">Loading…</div>
  return user ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Auth mode="login" />} />
      <Route path="/register" element={<Auth mode="register" />} />
      <Route path="/invite/:token" element={<AcceptInvite />} />
      <Route element={<Protected><Layout /></Protected>}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/my-day" element={<MyDay />} />
        <Route path="/my-work" element={<MyWork />} />
        <Route path="/tasks" element={<Tasks />} />
        <Route path="/projects" element={<Projects />} />
        <Route path="/projects/:id" element={<Project />} />
        <Route path="/reports" element={<Reports />} />
        <Route path="/team" element={<Team />} />
        <Route path="/settings" element={<Settings />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
