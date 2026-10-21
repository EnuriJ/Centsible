import { useState, useCallback } from 'react'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './context/useAuth'
import AuthForm from './components/AuthForm'
import Logo from './components/Logo'
import FinancialSummary from './components/FinancialSummary'
import SpendByCategoryChart from './components/SpendByCategoryChart'
import IncomeByCategoryTable from './components/IncomeByCategoryTable'
import CashFlowChart from './components/CashFlowChart'
import SpendOverTimeChart from './components/SpendOverTimeChart'
import CsvUploadForm from './components/CsvUploadForm'
import FiltersBar from './components/FiltersBar'
import DeleteAccountDialog from './components/DeleteAccountDialog'
import './App.css'

// The shared demo account can't be deleted (the server enforces this too)
const DEMO_EMAIL = 'demo@centsible.com'

function Dashboard() {
  const { user, logout } = useAuth()
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const [filters, setFilters] = useState({ startDate: '', endDate: '', category: '' })

  const handleUploadSuccess = useCallback(() => {
    setRefreshKey((key) => key + 1)
  }, [])

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header-row">
          <div className="app-brand">
            <Logo size={46} showText={false} />
            <div>
              <h1>Centsible</h1>
              <div className="app-header-statement">
                <span className="app-header-badge">Institutional Precision</span>
                <span>Sophisticated clarity for your personal wealth</span>
                <span style={{ color: 'var(--centsible-text-secondary)', opacity: 0.5 }}>&bull;</span>
                <span>Income &amp; spending telemetry</span>
              </div>
            </div>
          </div>

          <div className="app-user-nav">
            <div className="user-profile-pill">
              <span className="user-avatar-initial">
                {(user?.displayName || user?.email || 'U').charAt(0).toUpperCase()}
              </span>
              <div className="user-info-text">
                <span className="user-display-name">{user?.displayName || 'User'}</span>
                <span className="user-email-subtitle">{user?.email}</span>
              </div>
            </div>
            <button type="button" className="btn-signout" onClick={logout} title="Sign Out">
              Sign Out
            </button>
            {user?.email !== DEMO_EMAIL && (
              <button
                type="button"
                className="btn-delete-account"
                onClick={() => setShowDeleteDialog(true)}
                title="Permanently delete your account and data"
              >
                Delete account
              </button>
            )}
          </div>
        </div>
      </header>

      {showDeleteDialog && <DeleteAccountDialog onClose={() => setShowDeleteDialog(false)} />}

      <main className="app-main">
        {/* CSV / PDF Import Section */}
        <CsvUploadForm onUploadSuccess={handleUploadSuccess} />

        {/* Global Filter Bar */}
        <FiltersBar filters={filters} onChange={setFilters} />

        {/* Dynamic Financial Narrative Description & Metric Cards */}
        <FinancialSummary
          refreshKey={refreshKey}
          startDate={filters.startDate}
          endDate={filters.endDate}
          category={filters.category}
        />

        {/* Side-by-Side Category Breakdown Grid */}
        <div className="dashboard-grid">
          <SpendByCategoryChart
            refreshKey={refreshKey}
            startDate={filters.startDate}
            endDate={filters.endDate}
            category={filters.category}
          />

          <IncomeByCategoryTable
            refreshKey={refreshKey}
            startDate={filters.startDate}
            endDate={filters.endDate}
          />
        </div>

        {/* Cash Flow Comparison Chart (Monthly Income vs. Expenses vs. Net Savings) */}
        <CashFlowChart
          refreshKey={refreshKey}
          startDate={filters.startDate}
          endDate={filters.endDate}
          category={filters.category}
        />

        {/* Category-Specific Spending Trend */}
        <SpendOverTimeChart
          refreshKey={refreshKey}
          startDate={filters.startDate}
          endDate={filters.endDate}
          category={filters.category}
        />
      </main>
    </div>
  )
}

function AppContent() {
  const { isAuthenticated, loading } = useAuth()

  if (loading) {
    return (
      <div className="auth-loading-screen">
        <div className="auth-loading-spinner" />
        <p>Restoring institutional session…</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <AuthForm />
  }

  return <Dashboard />
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  )
}
