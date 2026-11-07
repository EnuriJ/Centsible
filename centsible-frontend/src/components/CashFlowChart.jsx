import {
  ComposedChart,
  Bar,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import { useAnalytics } from '../hooks/useAnalytics'
import { formatLKR, formatCompactLKR } from '../utils/currency'

export default function CashFlowChart({ refreshKey, startDate, endDate, category }) {
  const { data: loaded, status } = useAnalytics('/analytics/cash-flow-over-time', { startDate, endDate, category, refreshKey })
  const data = loaded ?? []

  if (status === 'loading') {
    return <p className="chart-message">Loading cash flow trend…</p>
  }

  if (status === 'error') {
    return (
      <p className="chart-message chart-message--error">
        Couldn't reach the backend to load cash flow data.
      </p>
    )
  }

  return (
    <div className="chart-card">
      <div className="cash-flow-header">
        <div>
          <h2>Monthly Cash Flow (Income vs. Expenses)</h2>
          <p className="chart-subtitle">Compare monthly inflows against spending and monitor your net savings trend in LKR</p>
        </div>
      </div>

      {data.length === 0 ? (
        <p className="chart-message">No transactions in this date range.</p>
      ) : (
        <ResponsiveContainer width="100%" height={340}>
          <ComposedChart data={data} margin={{ top: 12, right: 24, left: 8, bottom: 8 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
            <XAxis dataKey="month" tick={{ fontSize: 12 }} />
            <YAxis tickFormatter={formatCompactLKR} width={80} />
            <Tooltip
              formatter={(value, name) => [
                formatLKR(value),
                name === 'totalIncome' ? 'Income' : name === 'totalExpense' ? 'Expenses' : 'Net Savings',
              ]}
            />
            <Legend
              formatter={(value) =>
                value === 'totalIncome'
                  ? 'Income'
                  : value === 'totalExpense'
                  ? 'Expenses'
                  : 'Net Savings'
              }
            />
            <Bar
              dataKey="totalIncome"
              fill="#0D6E51"
              radius={[4, 4, 0, 0]}
              maxBarSize={32}
            />
            <Bar
              dataKey="totalExpense"
              fill="#E11D48"
              radius={[4, 4, 0, 0]}
              maxBarSize={32}
            />
            <Line
              type="monotone"
              dataKey="netSavings"
              stroke="#E5A93C"
              strokeWidth={3}
              dot={{ r: 4, fill: '#E5A93C', stroke: '#FFFFFF', strokeWidth: 2 }}
              activeDot={{ r: 6, fill: '#E5A93C', stroke: '#0F172A', strokeWidth: 2 }}
            />
          </ComposedChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}
