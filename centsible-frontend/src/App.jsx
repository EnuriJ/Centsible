import { useState, useCallback } from 'react'
import Logo from './components/Logo'
import SpendByCategoryChart from './components/SpendByCategoryChart'
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
          <Logo size={40} showText={false} />
          <div>
            <h1>Centsible</h1>
            <p>Smart finance made simple &bull; Your spending at a glance</p>
          </div>
        </div>
      </header>

      <main className="app-main">
        <CsvUploadForm onUploadSuccess={handleUploadSuccess} />

        <FiltersBar filters={filters} onChange={setFilters} />

        <SpendByCategoryChart
          refreshKey={refreshKey}
          startDate={filters.startDate}
          endDate={filters.endDate}
          category={filters.category}
        />

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
