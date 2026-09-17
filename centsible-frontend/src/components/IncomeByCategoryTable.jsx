import { useEffect, useState } from 'react'
import api from '../api/client'
import { formatLKR } from '../utils/currency'

export default function IncomeByCategoryTable({ refreshKey, startDate, endDate }) {
  const [data, setData] = useState([])
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    setStatus('loading')
    const params = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    api
      .get('/analytics/income-by-category', { params })
      .then((res) => {
        setData(res.data)
        setStatus('ready')
      })
      .catch((err) => {
        console.error('Failed to load income-by-category:', err)
        setStatus('error')
      })
  }, [refreshKey, startDate, endDate])

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
