import { useEffect, useState } from 'react'
import { useAuth } from '../context/useAuth'

export default function DeleteAccountDialog({ onClose }) {
  const { deleteAccount } = useAuth()
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape' && !submitting) onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, submitting])

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      // On success this signs the user out, which unmounts the dashboard (and this dialog).
      await deleteAccount(password)
    } catch (err) {
      setError(err?.response?.data?.error || 'Could not delete the account. Please try again.')
      setSubmitting(false)
    }
  }

  return (
    <div className="dialog-overlay" onClick={() => !submitting && onClose()}>
      <div
        className="dialog-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-dialog-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="delete-dialog-title">Delete your account?</h2>
        <p className="dialog-warning">
          This permanently deletes your account and every transaction, category and rule
          you&apos;ve imported. It can&apos;t be undone.
        </p>

        <form onSubmit={handleSubmit}>
          <label htmlFor="delete-password">Confirm your password</label>
          <input
            id="delete-password"
            type="password"
            required
            autoFocus
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          {error && (
            <div className="dialog-error" role="alert">
              {error}
            </div>
          )}

          <div className="dialog-actions">
            <button type="button" className="dialog-btn-cancel" onClick={onClose} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="dialog-btn-danger" disabled={submitting || !password}>
              {submitting ? 'Deleting…' : 'Delete everything'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
