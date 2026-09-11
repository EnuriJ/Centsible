import { useEffect, useState } from 'react'
import api from '../api/client'

export default function FiltersBar({ filters, onChange }) {
  const [categories, setCategories] = useState([])

  useEffect(() => {
    api
      .get('/categories')
      .then((res) => setCategories(res.data))
      .catch((err) => console.error('Failed to load categories:', err))
  }, [])

  function handleFieldChange(field, value) {
    onChange({ ...filters, [field]: value })
  }

  function handleReset() {
    onChange({ startDate: '', endDate: '', category: '' })
  }

  const hasActiveFilters = filters.startDate || filters.endDate || filters.category

  return (
    <div className="filters-bar">
      <div className="filter-field">
        <label htmlFor="startDate">From</label>
        <input
          id="startDate"
          type="date"
          value={filters.startDate}
          onChange={(e) => handleFieldChange('startDate', e.target.value)}
        />
      </div>

      <div className="filter-field">
        <label htmlFor="endDate">To</label>
        <input
          id="endDate"
          type="date"
          value={filters.endDate}
          onChange={(e) => handleFieldChange('endDate', e.target.value)}
        />
      </div>

      <div className="filter-field">
        <label htmlFor="category">Category</label>
        <select
          id="category"
          value={filters.category}
          onChange={(e) => handleFieldChange('category', e.target.value)}
        >
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
        <span className="filter-hint">applies to trend chart</span>
      </div>

      {hasActiveFilters && (
        <button type="button" className="filters-reset" onClick={handleReset}>
          Reset filters
        </button>
      )}
    </div>
  )
}
