import type { CSSProperties, ReactNode } from 'react'
import styles from './EventCover.module.css'

interface EventCoverProps {
  id: number
  title: string
  muted?: boolean
  initialSize?: string
  className?: string
  children?: ReactNode
}

function huesFor(id: number): [number, number] {
  const a = (id * 137) % 360
  return [a, (a + 48) % 360]
}

export function EventCover({ id, title, muted, initialSize = '9rem', className, children }: EventCoverProps) {
  const [hueA, hueB] = huesFor(id)
  const style = { '--hue-a': hueA, '--hue-b': hueB } as CSSProperties
  return (
    <div className={`${styles.cover} ${muted ? styles.muted : ''} ${className ?? ''}`} style={style}>
      <span className={styles.initial} style={{ fontSize: initialSize }} aria-hidden="true">
        {title.trim().charAt(0).toUpperCase()}
      </span>
      {children}
    </div>
  )
}
