import { dateTileParts } from '../format'
import styles from './DateTile.module.css'

export function DateTile({ iso, size = 'normal' }: { iso: string; size?: 'normal' | 'large' }) {
  const { month, day } = dateTileParts(iso)
  return (
    <time dateTime={iso} className={`${styles.tile} ${size === 'large' ? styles.large : ''}`}>
      <span className={styles.month}>{month}</span>
      <span className={styles.day}>{day}</span>
    </time>
  )
}
