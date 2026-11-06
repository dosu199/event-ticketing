import { useEffect, useState } from 'react'

/** Milliseconds left until `target` (ISO string), updated every second. Never negative. */
export function useCountdown(target: string | null | undefined): number {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (!target) return
    const tick = () => setNow(Date.now())
    const first = window.setTimeout(tick, 0) // sync right away when a new target arrives
    const timer = window.setInterval(tick, 1_000)
    return () => {
      window.clearTimeout(first)
      window.clearInterval(timer)
    }
  }, [target])

  return target ? Math.max(0, new Date(target).getTime() - now) : 0
}

export function formatCountdown(ms: number): string {
  const totalSeconds = Math.ceil(ms / 1_000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}
