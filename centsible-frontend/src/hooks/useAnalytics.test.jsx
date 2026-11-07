import { beforeEach, describe, expect, it, vi } from 'vitest'
import { act, render, screen } from '@testing-library/react'
import api from '../api/client'
import { useAnalytics } from './useAnalytics'
import FinancialSummary from '../components/FinancialSummary'

vi.mock('../api/client', () => ({
  default: { get: vi.fn() },
  AUTH_TOKEN_KEY: 'centsible_token',
}))

function Probe(props) {
  const { data, status } = useAnalytics('/analytics/test', props)
  return (
    <p>
      {status}:{data ? JSON.stringify(data) : 'none'}
    </p>
  )
}

beforeEach(() => {
  api.get.mockReset()
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

describe('useAnalytics', () => {
  it('is loading first, then ready with the data', async () => {
    api.get.mockResolvedValue({ data: { total: 3 } })
    render(<Probe />)

    expect(screen.getByText('loading:none')).toBeInTheDocument()
    expect(await screen.findByText('ready:{"total":3}')).toBeInTheDocument()
  })

  it('only sends the filters that are set', async () => {
    api.get.mockResolvedValue({ data: [] })
    render(<Probe startDate="2026-08-01" category="Groceries" />)
    await screen.findByText('ready:[]')

    expect(api.get).toHaveBeenCalledWith('/analytics/test', {
      params: { startDate: '2026-08-01', category: 'Groceries' },
    })
  })

  it('reports an error when the request fails', async () => {
    api.get.mockRejectedValue(new Error('Network Error'))
    render(<Probe />)

    expect(await screen.findByText('error:none')).toBeInTheDocument()
  })

  it('goes back to loading and re-fetches when a filter changes', async () => {
    api.get.mockResolvedValueOnce({ data: ['first'] })
    const { rerender } = render(<Probe startDate="2026-01-01" />)
    await screen.findByText('ready:["first"]')

    let release
    api.get.mockReturnValueOnce(new Promise((resolve) => (release = resolve)))
    rerender(<Probe startDate="2026-02-01" />)

    expect(screen.getByText('loading:none')).toBeInTheDocument()
    release({ data: ['second'] })
    expect(await screen.findByText('ready:["second"]')).toBeInTheDocument()
    expect(api.get).toHaveBeenCalledTimes(2)
  })

  it('ignores a slow answer to a request that has been replaced', async () => {
    let releaseOld
    api.get.mockReturnValueOnce(new Promise((resolve) => (releaseOld = resolve)))
    const { rerender } = render(<Probe startDate="2026-01-01" />)

    api.get.mockResolvedValueOnce({ data: ['new'] })
    rerender(<Probe startDate="2026-02-01" />)
    await screen.findByText('ready:["new"]')

    await act(async () => {
      releaseOld({ data: ['old'] })
    })
    expect(screen.getByText('ready:["new"]')).toBeInTheDocument()
  })
})

describe('FinancialSummary', () => {
  it('shows the headline figures in rupees', async () => {
    api.get.mockResolvedValue({
      data: {
        narrative: 'You saved well this month.',
        totalIncome: 100000,
        totalExpense: 40000,
        netSavings: 60000,
        incomeCount: 2,
        expenseCount: 9,
        savingsRate: 60,
      },
    })
    render(<FinancialSummary />)

    expect(screen.getByText(/Calculating financial summary/)).toBeInTheDocument()
    expect(await screen.findByText('You saved well this month.')).toBeInTheDocument()
    expect(screen.getByText('Rs. 100,000.00')).toBeInTheDocument()
    expect(screen.getByText('Surplus')).toBeInTheDocument()
    expect(screen.getByText('60.0%')).toBeInTheDocument()
  })

  it('shows a deficit when spending is higher than income', async () => {
    api.get.mockResolvedValue({
      data: {
        narrative: 'Spending ran ahead of income.',
        totalIncome: 10000,
        totalExpense: 15000,
        netSavings: -5000,
        incomeCount: 1,
        expenseCount: 4,
        savingsRate: null,
      },
    })
    render(<FinancialSummary />)

    expect(await screen.findByText('Deficit')).toBeInTheDocument()
    expect(screen.getByText('Expenses exceed income')).toBeInTheDocument()
  })
})
