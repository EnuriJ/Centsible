import { useEffect, useState } from 'react'
import api from '../api/client'

/**
 * Loads one analytics endpoint and re-loads whenever the filters (or refreshKey) change.
 *
 * Returns { data, status } where status is 'loading' | 'ready' | 'error'.
 *
 * "Loading" is worked out while rendering, by checking whether the stored result belongs to
 * the current request, so the effect never has to set state synchronously. A response that
 * arrives after the filters have changed again is ignored.
 */
export function useAnalytics(endpoint, { startDate, endDate, category, refreshKey } = {}) {
  const requestKey = JSON.stringify([endpoint, startDate, endDate, category, refreshKey])
  const [result, setResult] = useState({ key: null, data: null, failed: false })

  useEffect(() => {
    let cancelled = false
    const params = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    if (category) params.category = category

    api
      .get(endpoint, { params })
      .then((res) => {
        if (!cancelled) setResult({ key: requestKey, data: res.data, failed: false })
      })
      .catch((err) => {
        console.error(`Failed to load ${endpoint}:`, err)
        if (!cancelled) setResult({ key: requestKey, data: null, failed: true })
      })

    return () => {
      cancelled = true
    }
  }, [requestKey, endpoint, startDate, endDate, category])

  if (result.key !== requestKey) return { data: null, status: 'loading' }
  return { data: result.data, status: result.failed ? 'error' : 'ready' }
}
