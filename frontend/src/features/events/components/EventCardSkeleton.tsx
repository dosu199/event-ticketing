import styles from './EventCardSkeleton.module.css'

export function EventCardSkeleton() {
  return (
    <div className={styles.card} aria-hidden="true">
      <div className={styles.cover} />
      <div className={styles.body}>
        <div className={`${styles.line} ${styles.title}`} />
        <div className={styles.line} />
        <div className={`${styles.line} ${styles.short}`} />
      </div>
    </div>
  )
}
