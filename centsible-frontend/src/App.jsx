import { useState, useCallback } from 'react'
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
        <h1>Centsible</h1>
        <p>Your spending, at a glance</p>
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
