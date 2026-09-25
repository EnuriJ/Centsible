import { useState, useCallback } from 'react'
import Logo from './components/Logo'
import FinancialSummary from './components/FinancialSummary'
import SpendByCategoryChart from './components/SpendByCategoryChart'
import IncomeByCategoryTable from './components/IncomeByCategoryTable'
import CashFlowChart from './components/CashFlowChart'
import SpendOverTimeChart from './components/SpendOverTimeChart'
import CsvUploadForm from './components/CsvUploadForm'
import FiltersBar from './components/FiltersBar'
import './App.css'

function App() {
  const [refreshKey, setRefreshKey] = useState(0)
  const [filters, setFilters] = useState({ startDate: '', endDate: '', category: '' })

  const handleUploadSuccess = useCallback(() => {
    setRefreshKey((key) => key + 1)
  }, [])

  return (
    <div className="app">
      <header className="app-header">
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
      </header>

      <main className="app-main">
        {/* CSV Import Section */}
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

export default App
