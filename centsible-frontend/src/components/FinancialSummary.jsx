import { useEffect, useState } from 'react'
import api from '../api/client'
import { formatLKR } from '../utils/currency'

export default function FinancialSummary({ refreshKey, startDate, endDate, category }) {
  const [summary, setSummary] = useState(null)
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    setStatus('loading')
    const params = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    if (category) params.category = category

    api
      .get('/analytics/summary', { params })
      .then((res) => {
        setSummary(res.data)
        setStatus('ready')
      })
      .catch((err) => {
        console.error('Failed to load financial summary:', err)
        setStatus('error')
      })
  }, [refreshKey, startDate, endDate, category])

  if (status === 'loading') {
    return (
      <div className="summary-section">
        <div className="summary-banner summary-banner--loading">
          <p>Calculating financial summary…</p>
        </div>
      </div>
    )
  }

  if (status === 'error' || !summary) {
    return null
  }

  const isPositiveSavings = (summary.netSavings ?? 0) >= 0

  return (
    <div className="summary-section">
      {/* Narrative Description Banner */}
      <div className="summary-banner">
        <div className="summary-banner__icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"
              fill="#0D6E51"
              stroke="#0D6E51"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
        <div className="summary-banner__content">
          <h3>Financial Overview</h3>
          <p>{summary.narrative}</p>
        </div>
      </div>

      {/* KPI Metric Cards */}
      <div className="metrics-grid">
        <div className="metric-card metric-card--income">
          <div className="metric-card__header">
            <span className="metric-label">Total Income</span>
            <span className="metric-badge metric-badge--income">+{summary.incomeCount} txns</span>
          </div>
          <div className="metric-value">{formatLKR(summary.totalIncome)}</div>
          {summary.topIncomeCategory && (
            <div className="metric-subtext">
              Primary: <strong>{summary.topIncomeCategory}</strong> ({formatLKR(summary.topIncomeAmount)})
            </div>
          )}
        </div>

        <div className="metric-card metric-card--expense">
          <div className="metric-card__header">
            <span className="metric-label">Total Expenses</span>
            <span className="metric-badge metric-badge--expense">-{summary.expenseCount} txns</span>
          </div>
          <div className="metric-value">{formatLKR(summary.totalExpense)}</div>
          {summary.topExpenseCategory && (
            <div className="metric-subtext">
              Top: <strong>{summary.topExpenseCategory}</strong> ({summary.topExpensePercentage?.toFixed(1)}%)
            </div>
          )}
        </div>

        <div className="metric-card metric-card--savings">
          <div className="metric-card__header">
            <span className="metric-label">Net Cash Flow</span>
            <span className={`metric-badge ${isPositiveSavings ? 'metric-badge--savings' : 'metric-badge--deficit'}`}>
              {isPositiveSavings ? 'Surplus' : 'Deficit'}
            </span>
          </div>
          <div className={`metric-value ${isPositiveSavings ? 'metric-value--positive' : 'metric-value--negative'}`}>
            {isPositiveSavings ? '+' : ''}{formatLKR(summary.netSavings)}
          </div>
          <div className="metric-subtext">
            {isPositiveSavings ? 'Retained savings after expenses' : 'Expenses exceed income'}
          </div>
        </div>

        <div className="metric-card metric-card--rate">
          <div className="metric-card__header">
            <span className="metric-label">Savings Rate</span>
            <span className="metric-badge metric-badge--rate">KPI</span>
          </div>
          <div className="metric-value">
            {summary.savingsRate !== null ? `${summary.savingsRate.toFixed(1)}%` : '0.0%'}
          </div>
          <div className="metric-subtext">
            of total income saved
          </div>
        </div>
      </div>
    </div>
  )
}
