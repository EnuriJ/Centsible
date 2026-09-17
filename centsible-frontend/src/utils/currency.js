/**
 * Currency utilities for Sri Lankan Rupee (LKR / Rs.)
 */
export function formatLKR(value, options = {}) {
  if (value === null || value === undefined || isNaN(Number(value))) {
    return 'Rs. 0.00'
  }
  const num = Number(value)
  const maxFraction = options.maximumFractionDigits ?? 2
  const minFraction = options.minimumFractionDigits ?? 2

  const formatted = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: minFraction,
    maximumFractionDigits: maxFraction,
  }).format(Math.abs(num))

  const sign = num < 0 ? '-' : ''
  return `${sign}Rs. ${formatted}`
}

export function formatCompactLKR(value) {
  if (value === null || value === undefined || isNaN(Number(value))) {
    return 'Rs. 0'
  }
  const num = Number(value)
  const sign = num < 0 ? '-' : ''
  const abs = Math.abs(num)
  if (abs >= 1_000_000) {
    return `${sign}Rs. ${(abs / 1_000_000).toFixed(1)}M`
  }
  if (abs >= 1_000) {
    return `${sign}Rs. ${(abs / 1_000).toFixed(0)}k`
  }
  return `${sign}Rs. ${abs.toFixed(0)}`
}
