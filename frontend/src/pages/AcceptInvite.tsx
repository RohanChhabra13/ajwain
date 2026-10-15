import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { api, errorMessage } from '../api'
import type { InvitationInfo } from '../types'
import { useApp } from '../auth'

/** For people who already have an account and open an invitation link. */
export default function AcceptInvite() {
  const { token } = useParams()
  const { user, loading, loadWorkspaces } = useApp()
  const nav = useNavigate()
  const [info, setInfo] = useState<InvitationInfo | null>(null)
  const [error, setError] = useState('')

  useEffect(() => { api.get<InvitationInfo>(`/invitations/${token}`).then(setInfo).catch((e) => setError(errorMessage(e))) }, [token])

  if (loading) return <div className="center muted">Loading…</div>
  if (!user && info) return <Navigate to={`/register?invite=${token}`} replace />

  const accept = async () => {
    try { await api.post(`/invitations/${token}/accept`); await loadWorkspaces(); nav('/') } catch (e) { setError(errorMessage(e)) }
  }
  return (
    <div className="center">
      <div className="card auth">
        <h1>Workspace invitation</h1>
        {info && <p>Join <b>{info.organizationName} / {info.workspaceName}</b> as {info.email}?</p>}
        {error && <div className="error">{error}</div>}
        {info && <button onClick={accept}>Accept invitation</button>}
        <Link to="/">Back to Workboard</Link>
      </div>
    </div>
  )
}
