import { useState } from 'react'
import { useAuth } from '../context/useAuth'
import Logo from './Logo'

export default function AuthForm() {
  const { login, signup } = useAuth()
  const [mode, setMode] = useState('login') // 'login' | 'signup'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      if (mode === 'login') {
        await login(email, password)
      } else {
        await signup(email, password, displayName)
      }
    } catch (err) {
      console.error('Auth request failed:', err)
      const message =
        err.response?.data?.error ||
        err.response?.data?.message ||
        'Authentication failed. Please verify your details and try again.'
      setError(message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDemoLogin() {
    setError(null)
    setSubmitting(true)
    try {
      await login('demo@centsible.com', 'Password123!')
    } catch (err) {
      console.error('Demo login failed:', err)
      setError('Could not access demo portfolio. Ensure the backend server is running.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-brand-center">
          <Logo size={52} showText={false} />
          <h2>Centsible</h2>
          <div className="auth-subtitle">
            <span className="app-header-badge">Institutional Precision</span>
            <span>Personal wealth intelligence</span>
          </div>
        </div>

        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab ${mode === 'login' ? 'auth-tab--active' : ''}`}
            onClick={() => {
              setMode('login')
              setError(null)
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            className={`auth-tab ${mode === 'signup' ? 'auth-tab--active' : ''}`}
            onClick={() => {
              setMode('signup')
              setError(null)
            }}
          >
            Create Account
          </button>
        </div>

        {error && (
          <div className="auth-error-banner" role="alert">
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          {mode === 'signup' && (
            <div className="auth-field">
              <label htmlFor="auth-name">Full / Display Name</label>
              <input
                id="auth-name"
                type="text"
                placeholder="e.g. Eleanor Vance"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                autoComplete="name"
              />
            </div>
          )}

          <div className="auth-field">
            <label htmlFor="auth-email">Email Address</label>
            <input
              id="auth-email"
              type="email"
              required
              placeholder="name@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
            />
          </div>

          <div className="auth-field">
            <label htmlFor="auth-password">Password</label>
            <input
              id="auth-password"
              type="password"
              required
              placeholder={mode === 'signup' ? 'At least 10 characters' : 'Enter your password'}
              minLength={mode === 'signup' ? 10 : undefined}
              maxLength={mode === 'signup' ? 72 : undefined}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
            />
          </div>

          <button
            type="submit"
            className="auth-btn-primary"
            disabled={submitting}
          >
            {submitting
              ? mode === 'login'
                ? 'Signing in…'
                : 'Creating account…'
              : mode === 'login'
              ? 'Sign In to Centsible'
              : 'Create Account'}
          </button>
        </form>

        <div className="auth-divider">
          <span>or test drive instantly</span>
        </div>

        <button
          type="button"
          className="auth-btn-demo"
          onClick={handleDemoLogin}
          disabled={submitting}
        >
          Explore Demo Portfolio
        </button>
      </div>
    </div>
  )
}
