import { useEffect, useState } from 'react'
import {
  BarChart,
  Bar,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts'
import api from '../api/client'

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(value)
}

const HIGHLIGHT_COLOR = '#4f46e5'
const DIMMED_COLOR = '#d8d9f5'

// Note: `category` is only used here to highlight a bar, not to refetch -
// the data itself always includes all categories, since that's the axis
// this chart groups by.
export default function SpendByCategoryChart({ refreshKey, startDate, endDate, category }) {
  const [data, setData] = useState([])
  const [status, setStatus] = useState('loading') // 'loading' | 'ready' | 'error'

  useEffect(() => {
    setStatus('loading')
    const params = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    api
      .get('/analytics/spend-by-category', { params })
      .then((res) => {
        setData(res.data)
        setStatus('ready')
      })
      .catch((err) => {
        console.error('Failed to load spend-by-category:', err)
        setStatus('error')
      })
  }, [refreshKey, startDate, endDate])

  if (status === 'loading') {
    return <p className="chart-message">Loading spending data…</p>
  }

  if (status === 'error') {
    return (
      <p className="chart-message chart-message--error">
        Couldn't reach the backend. Is it running at localhost:8080?
      </p>
    )
  }

  return (
    <div className="chart-card">
      <h2>Spend by Category</h2>
      {data.length === 0 ? (
        <p className="chart-message">No transactions in this range.</p>
      ) : (
        <ResponsiveContainer width="100%" height={360}>
          <BarChart data={data} margin={{ top: 8, right: 24, left: 8, bottom: 8 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="category" tick={{ fontSize: 12 }} />
            <YAxis tickFormatter={formatCurrency} width={70} />
            <Tooltip formatter={(value) => formatCurrency(value)} />
            <Bar dataKey="totalSpent" radius={[4, 4, 0, 0]}>
              {data.map((entry) => (
                <Cell
                  key={entry.category}
                  fill={
                    !category || entry.category === category
                      ? HIGHLIGHT_COLOR
                      : DIMMED_COLOR
                  }
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}
