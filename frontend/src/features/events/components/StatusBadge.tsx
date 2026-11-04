import { statusLabel } from '../format'
import type { EventStatus } from '../types'
import styles from './StatusBadge.module.css'

interface StatusBadgeProps {
  status: EventStatus
  onImage?: boolean
}

export function StatusBadge({ status, onImage }: StatusBadgeProps) {
  return (
    <span className={`${styles.badge} ${styles[status]} ${onImage ? styles.onImage : ''}`}>
      <span className={styles.dot} aria-hidden="true" />
      {statusLabel[status]}
    </span>
  )
}
