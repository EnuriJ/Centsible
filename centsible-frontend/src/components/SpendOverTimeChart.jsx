import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts'
import { useAnalytics } from '../hooks/useAnalytics'
import { formatLKR, formatCompactLKR } from '../utils/currency'

export default function SpendOverTimeChart({ refreshKey, startDate, endDate, category }) {
  const { data: loaded, status } = useAnalytics('/analytics/spend-over-time', { startDate, endDate, category, refreshKey })
  const data = loaded ?? []

  if (status === 'loading') {
    return <p className="chart-message">Loading spending trend…</p>
  }

  if (status === 'error') {
    return (
      <p className="chart-message chart-message--error">
        Couldn't reach the backend. Is it running and reachable?
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
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
            <XAxis dataKey="month" tick={{ fontSize: 12 }} />
            <YAxis tickFormatter={formatCompactLKR} width={80} />
            <Tooltip formatter={(value) => [formatLKR(value), 'Total Spent']} />
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
