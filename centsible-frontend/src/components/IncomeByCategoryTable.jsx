import { useAnalytics } from '../hooks/useAnalytics'
import { formatLKR } from '../utils/currency'

export default function IncomeByCategoryTable({ refreshKey, startDate, endDate }) {
  const { data: loaded, status } = useAnalytics('/analytics/income-by-category', {
    startDate,
    endDate,
    refreshKey,
  })
  const data = loaded ?? []

  const totalIncome = data.reduce((sum, item) => sum + (Number(item.totalIncome) || 0), 0)

  return (
    <div className="chart-card income-breakdown-card">
      <div className="income-card-header">
        <h2>Income Sources</h2>
        {status === 'ready' && data.length > 0 && (
          <span className="income-card-total">
            Total: <strong>{formatLKR(totalIncome)}</strong>
          </span>
        )}
      </div>

      {status === 'loading' && <p className="chart-message">Loading income breakdown…</p>}

      {status === 'error' && (
        <p className="chart-message chart-message--error">
          Couldn't load income sources.
        </p>
      )}

      {status === 'ready' && data.length === 0 && (
        <p className="chart-message">No income transactions in this date range.</p>
      )}

      {status === 'ready' && data.length > 0 && (
        <div className="income-list">
          {data.map((item) => (
            <div key={item.category} className="income-row">
              <div className="income-row__info">
                <div className="income-row__title">
                  <span className="income-category-name">{item.category}</span>
                  <span className="income-category-count">
                    {item.count} deposit{item.count === 1 ? '' : 's'}
                  </span>
                </div>
                <div className="income-row__amount">
                  <strong>{formatLKR(item.totalIncome)}</strong>
                  <span className="income-percentage">{item.percentage?.toFixed(1)}%</span>
                </div>
              </div>
              <div className="income-progress-bar">
                <div
                  className="income-progress-fill"
                  style={{ width: `${Math.min(100, Math.max(0, item.percentage ?? 0))}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
