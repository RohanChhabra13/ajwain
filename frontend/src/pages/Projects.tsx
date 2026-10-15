import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, errorMessage } from '../api'
import type { Project } from '../types'
import { useApp } from '../auth'

export default function Projects() {
  const { workspaceId } = useApp()
  const [projects, setProjects] = useState<Project[]>([])
  const [name, setName] = useState('')
  const [showArchived, setShowArchived] = useState(false)
  const [error, setError] = useState('')

  const load = () => api.get<Project[]>(`/workspaces/${workspaceId}/projects`).then(setProjects)
  useEffect(() => { load() }, [workspaceId])

  const create = async (e: FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    try { await api.post(`/workspaces/${workspaceId}/projects`, { name }); setName(''); load() } catch (err) { setError(errorMessage(err)) }
  }
  const list = projects.filter((p) => showArchived || p.status === 'ACTIVE')

  return (
    <>
      <div className="row between"><h1>Projects</h1>
        <label className="inline"><input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} /> Show archived</label>
      </div>
      <form className="row" onSubmit={create}>
        <input placeholder="New project name" value={name} onChange={(e) => setName(e.target.value)} />
        <button>Create</button>
      </form>
      {error && <div className="error">{error}</div>}
      <div className="grid">
        {list.map((p) => (
          <Link key={p.id} to={`/projects/${p.id}`} className="card project">
            <b>{p.name}</b>{p.status === 'ARCHIVED' && <span className="badge"> archived</span>}
            <div className="muted small">{p.description}</div>
          </Link>
        ))}
        {list.length === 0 && <p className="muted">No projects yet. Create your first one above.</p>}
      </div>
    </>
  )
}
