import { Link } from 'react-router'
import { Icon } from '../../../components/Icon'
import { formatCardDate } from '../format'
import type { EventSummary } from '../types'
import { DateTile } from './DateTile'
import { EventCover } from './EventCover'
import { StatusBadge } from './StatusBadge'
import styles from './EventCard.module.css'

export function EventCard({ event }: { event: EventSummary }) {
  const cancelled = event.status === 'CANCELLED'
  return (
    <article className={`${styles.card} ${cancelled ? styles.cancelled : ''}`}>
      <EventCover id={event.id} title={event.title} muted={cancelled} className={styles.cover}>
        <div className={styles.coverOverlay}>
          <DateTile iso={event.startTime} />
          <StatusBadge status={event.status} onImage />
        </div>
      </EventCover>

      <div className={styles.body}>
        <h2 className={styles.title}>
          <Link to={`/events/${event.id}`} className={styles.link}>
            {event.title}
          </Link>
        </h2>
        <div className={styles.meta}>
          <span className={styles.metaRow}>
            <Icon name="clock" size={16} />
            {formatCardDate(event.startTime)}
          </span>
          <span className={styles.metaRow}>
            <Icon name="pin" size={16} />
            {event.venue.name}, {event.venue.city}
          </span>
        </div>
      </div>

      <div className={styles.footer} aria-hidden="true">
        <span>{cancelled ? 'View details' : 'See event'}</span>
        <Icon name="arrowRight" size={16} />
      </div>
    </article>
  )
}
