import { useEffect, useState } from 'react'
import { get } from '../services/request'

export const AUTH_REVIEW_CHANGED = 'auth-review-changed'

export function usePendingAuthCount() {
  const [count, setCount] = useState<number | undefined>()

  useEffect(() => {
    let disposed = false
    let busy = false
    let refreshAgain = false
    const refresh = async () => {
      if (disposed || document.hidden) return
      if (busy) {
        refreshAgain = true
        return
      }
      busy = true
      try {
        const value = await get<number>('/admin/api/kpi/pending-auth')
        if (!disposed) setCount(Number.isSafeInteger(value) && value >= 0 ? value : undefined)
      } catch {
        // Unknown is not zero: hide stale counts until the next successful poll.
        if (!disposed) setCount(undefined)
      } finally {
        busy = false
        if (refreshAgain && !disposed) {
          refreshAgain = false
          void refresh()
        }
      }
    }
    void refresh()
    const timer = window.setInterval(() => void refresh(), 60000)
    document.addEventListener('visibilitychange', refresh)
    window.addEventListener('focus', refresh)
    window.addEventListener(AUTH_REVIEW_CHANGED, refresh)
    return () => {
      disposed = true
      window.clearInterval(timer)
      document.removeEventListener('visibilitychange', refresh)
      window.removeEventListener('focus', refresh)
      window.removeEventListener(AUTH_REVIEW_CHANGED, refresh)
    }
  }, [])

  return count
}
