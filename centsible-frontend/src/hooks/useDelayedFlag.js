import { useEffect, useState } from 'react'

/**
 * True once `active` has stayed true for `delayMs`. Resets as soon as `active` goes false.
 * Used to show a "this is taking a while" note only when a request really is slow.
 */
export function useDelayedFlag(active, delayMs) {
  const [elapsed, setElapsed] = useState(false)

  useEffect(() => {
    if (!active) return undefined
    const timer = setTimeout(() => setElapsed(true), delayMs)
    return () => {
      clearTimeout(timer)
      setElapsed(false)
    }
  }, [active, delayMs])

  return active && elapsed
}
