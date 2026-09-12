import { useEffect, useState } from 'react'
import {
  LineChart,
  Line,
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

export default function SpendOverTimeChart({ refreshKey, startDate, endDate, category }) {
  const [data, setData] = useState([])
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    setStatus('loading')
    const params = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    if (category) params.category = category

    api
      .get('/analytics/spend-over-time', { params })
      .then((res) => {
        setData(res.data)
        setStatus('ready')
      })
      .catch((err) => {
        console.error('Failed to load spend-over-time:', err)
        setStatus('error')
      })
  }, [refreshKey, startDate, endDate, category])

  if (status === 'loading') {
    return <p className="chart-message">Loading spending trend…</p>
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
      <h2>Spend Over Time{category ? ` — ${category}` : ''}</h2>
      {data.length === 0 ? (
        <p className="chart-message">No transactions in this range.</p>
      ) : (
        <ResponsiveContainer width="100%" height={320}>
          <LineChart data={data} margin={{ top: 8, right: 24, left: 8, bottom: 8 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="month" tick={{ fontSize: 12 }} />
            <YAxis tickFormatter={formatCurrency} width={70} />
            <Tooltip formatter={(value) => formatCurrency(value)} />
            <Line
              type="monotone"
              dataKey="totalSpent"
              stroke="#0D6E51"
              strokeWidth={2.5}
              dot={{ r: 4, fill: '#0D6E51' }}
              activeDot={{ r: 6, fill: '#E5A93C', stroke: '#FFFFFF', strokeWidth: 2 }}
            />
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}
