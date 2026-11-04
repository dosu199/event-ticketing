import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { Icon, type IconName } from './Icon'
import styles from './StatusMessage.module.css'

interface StatusMessageProps {
  title: string
  children?: ReactNode
  variant?: 'info' | 'error'
  icon?: IconName
  onRetry?: () => void
  backTo?: { to: string; label: string }
}

export function StatusMessage({ title, children, variant = 'info', icon, onRetry, backTo }: StatusMessageProps) {
  const isError = variant === 'error'
  return (
    <div className={`${styles.box} ${isError ? styles.error : ''}`} role={isError ? 'alert' : 'status'}>
      <span className={styles.icon}>
        <Icon name={icon ?? (isError ? 'alert' : 'search')} size={22} />
      </span>
      <h2 className={styles.title}>{title}</h2>
      {children && <p className={styles.text}>{children}</p>}
      {(onRetry || backTo) && (
        <div className={styles.actions}>
          {onRetry && (
            <button type="button" className={styles.button} onClick={onRetry}>
              Try again
            </button>
          )}
          {backTo && (
            <Link to={backTo.to} className={styles.button}>
              {backTo.label}
            </Link>
          )}
        </div>
      )}
    </div>
  )
}
