import { describe, expect, it } from 'vitest'
import { formatCompactLKR, formatLKR } from './currency'
import { DEMO_EMAIL, isDemoUser } from './demo'

describe('formatLKR', () => {
  it('formats amounts with Rs., thousands separators and two decimals', () => {
    expect(formatLKR(1234.5)).toBe('Rs. 1,234.50')
  })

  it('puts the minus sign before Rs. for negatives', () => {
    expect(formatLKR(-2629)).toBe('-Rs. 2,629.00')
  })

  it('treats missing or non-numeric values as zero', () => {
    expect(formatLKR(null)).toBe('Rs. 0.00')
    expect(formatLKR(undefined)).toBe('Rs. 0.00')
    expect(formatLKR('abc')).toBe('Rs. 0.00')
  })
})

describe('formatCompactLKR', () => {
  it('shortens thousands and millions for chart axes', () => {
    expect(formatCompactLKR(950)).toBe('Rs. 950')
    expect(formatCompactLKR(45_000)).toBe('Rs. 45k')
    expect(formatCompactLKR(2_500_000)).toBe('Rs. 2.5M')
    expect(formatCompactLKR(-45_000)).toBe('-Rs. 45k')
  })
})

describe('isDemoUser', () => {
  it('recognises the demo account regardless of letter case', () => {
    expect(isDemoUser({ email: DEMO_EMAIL })).toBe(true)
    expect(isDemoUser({ email: DEMO_EMAIL.toUpperCase() })).toBe(true)
  })

  it('is false for other users and for nobody', () => {
    expect(isDemoUser({ email: 'someone@example.com' })).toBe(false)
    expect(isDemoUser(null)).toBe(false)
  })
})
