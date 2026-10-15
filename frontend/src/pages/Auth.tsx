import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { api, errorMessage } from '../api'
import type { InvitationInfo } from '../types'
import { useApp } from '../auth'

export default function Auth({ mode }: { mode: 'login' | 'register' }) {
  const { user, login, register } = useApp()
  const nav = useNavigate()
  const [params] = useSearchParams()
  const inviteToken = params.get('invite') || ''
  const [invite, setInvite] = useState<InvitationInfo | null>(null)
  const [f, setF] = useState({ name: '', email: '', password: '', organizationName: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const isReg = mode === 'register'

  useEffect(() => {
    if (!isReg || !inviteToken) return
    api.get<InvitationInfo>(`/invitations/${inviteToken}`).then((i) => { setInvite(i); setF((x) => ({ ...x, email: i.email })) }).catch((e) => setError(errorMessage(e)))
  }, [isReg, inviteToken])

  if (user) return <Navigate to="/" replace />
  const set = (k: keyof typeof f) => (e: ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value })
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(''); setBusy(true)
    try {
      await (isReg ? register({ ...f, inviteToken: invite ? inviteToken : undefined }) : login({ email: f.email, password: f.password }))
      nav(params.get('next') || '/')
    } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }

  return (
    <div className="center">
      <form className="card auth" onSubmit={submit}>
        <div className="brand">Workboard</div>
        <h1>{isReg ? (invite ? `Join ${invite.organizationName}` : 'Create your team workspace') : 'Welcome back'}</h1>
        {invite && <p className="muted small">You've been invited to <b>{invite.workspaceName}</b>. Create your account to get started.</p>}
        {isReg && <input placeholder="Your name" value={f.name} onChange={set('name')} required autoComplete="name" />}
        {isReg && !invite && <input placeholder="Business / team name (optional)" value={f.organizationName} onChange={set('organizationName')} />}
        <input type="email" placeholder="Email" value={f.email} onChange={set('email')} required readOnly={!!invite} autoComplete="email" />
        <input type="password" placeholder="Password (min 8 characters)" value={f.password} onChange={set('password')} minLength={8} required autoComplete={isReg ? 'new-password' : 'current-password'} />
        {error && <div className="error">{error}</div>}
        <button disabled={busy}>{busy ? '…' : isReg ? 'Sign up' : 'Log in'}</button>
        <div className="muted small">
          {isReg ? <>Have an account? <Link to={inviteToken ? `/login?next=/invite/${inviteToken}` : '/login'}>Log in</Link></> : <>New here? <Link to="/register">Create an account</Link></>}
        </div>
      </form>
    </div>
  )
}
