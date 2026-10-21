import { useState, useEffect, useCallback } from 'react'
import api, { AUTH_TOKEN_KEY } from '../api/client'
import { AuthContext } from './authContextDef'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(() => localStorage.getItem(AUTH_TOKEN_KEY))
  const [loading, setLoading] = useState(() => Boolean(localStorage.getItem(AUTH_TOKEN_KEY)))

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    setToken(null)
    setUser(null)
    setLoading(false)
  }, [])

  // Restore authenticated session on page load if token is cached
  useEffect(() => {
    const savedToken = localStorage.getItem(AUTH_TOKEN_KEY)
    if (!savedToken) return

    api
      .get('/auth/me')
      .then((res) => {
        setUser(res.data)
        setToken(savedToken)
      })
      .catch((err) => {
        console.warn('Session restoration failed:', err?.response?.data || err.message)
        logout()
      })
      .finally(() => {
        setLoading(false)
      })
  }, [logout])

  // Listen for 401 unauthorized events from axios interceptor
  useEffect(() => {
    function handleUnauthorized() {
      logout()
    }
    window.addEventListener('centsible:unauthorized', handleUnauthorized)
    return () => {
      window.removeEventListener('centsible:unauthorized', handleUnauthorized)
    }
  }, [logout])

  const login = async (email, password) => {
    const res = await api.post('/auth/login', { email, password })
    const { token: receivedToken, user: receivedUser } = res.data
    localStorage.setItem(AUTH_TOKEN_KEY, receivedToken)
    setToken(receivedToken)
    setUser(receivedUser)
    return receivedUser
  }

  const signup = async (email, password, displayName) => {
    const res = await api.post('/auth/signup', { email, password, displayName })
    const { token: receivedToken, user: receivedUser } = res.data
    localStorage.setItem(AUTH_TOKEN_KEY, receivedToken)
    setToken(receivedToken)
    setUser(receivedUser)
    return receivedUser
  }

  // Permanently deletes the account and all its data on the server, then signs out locally.
  // Errors (e.g. wrong password) propagate to the caller so the dialog can show them.
  const deleteAccount = async (password) => {
    await api.delete('/account', { data: { password } })
    logout()
  }

  const value = {
    user,
    token,
    isAuthenticated: Boolean(user && token),
    loading,
    login,
    signup,
    logout,
    deleteAccount,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
