import { Link, useParams } from 'react-router'
import { ApiError } from '../api/http'
import { Icon } from '../components/Icon'
import { StatusMessage } from '../components/StatusMessage'
import { DateTile } from '../features/events/components/DateTile'
import { PriceList } from '../features/events/components/PriceList'
import { SeatingPlan } from '../features/events/components/SeatingPlan'
import { SeatPicker } from '../features/booking/components/SeatPicker'
import { EventCover } from '../features/events/components/EventCover'
import { StatusBadge } from '../features/events/components/StatusBadge'
import { formatFullDate, formatTime, parseEventId } from '../features/events/format'
import { useEvent, useSeats } from '../features/events/queries'
import type { EventSummary, Seat } from '../features/events/types'
import styles from './EventDetailPage.module.css'

function BackLink() {
  return (
    <Link to="/" className={styles.back}>
      <Icon name="arrowLeft" size={16} />
      All events
    </Link>
  )
}

interface SeatsState {
  seats: Seat[] | undefined
  isPending: boolean
  isError: boolean
}

function TicketPanel({ event, seatsState }: { event: EventSummary; seatsState: SeatsState }) {
  if (event.status === 'CANCELLED') {
    return (
      <p className={`${styles.notice} ${styles.noticeDanger}`} role="note">
        <Icon name="alert" size={18} />
        This event has been cancelled. Tickets are no longer available.
      </p>
    )
  }
  if (event.status === 'DRAFT') {
    return (
      <p className={`${styles.notice} ${styles.noticeWarning}`} role="note">
        <Icon name="clock" size={18} />
        This event isn't on sale yet. Check back once it's published.
      </p>
    )
  }
  const { seats, isPending, isError } = seatsState
  return (
    <>
      {isPending ? (
        <div className={styles.skeletonLine} style={{ height: 44 }} />
      ) : isError ? (
        <p className={`${styles.notice} ${styles.noticeInfo}`}>
          <Icon name="alert" size={18} />
          Prices couldn't be loaded right now.
        </p>
      ) : seats && seats.length > 0 ? (
        <PriceList seats={seats} />
      ) : (
        <p className={`${styles.notice} ${styles.noticeInfo}`}>
          <Icon name="ticket" size={18} />
          Prices will be announced soon.
        </p>
      )}
      {seats && seats.length > 0 && (
        <a href="#seating" className={styles.cta}>
          <Icon name="ticket" size={18} />
          Choose seats
        </a>
      )}
    </>
  )
}

export function EventDetailPage() {
  const { eventId } = useParams()
  const id = parseEventId(eventId)
  const { data: event, isPending, isError, error, refetch } = useEvent(id)
  const seatsQuery = useSeats(id)

  if (id === null || (isError && error instanceof ApiError && error.isNotFound)) {
    return (
      <>
        <title>Event not found · Event Ticketing</title>
        <BackLink />
        <StatusMessage title="Event not found" backTo={{ to: '/', label: 'Browse events' }}>
          This event doesn't exist or is no longer available.
        </StatusMessage>
      </>
    )
  }

  if (isPending) {
    return (
      <div aria-busy="true" aria-label="Loading event">
        <BackLink />
        <div className={styles.skeletonHero} />
        <div className={styles.layout}>
          <div className={styles.section}>
            <div className={styles.skeletonLine} style={{ width: '40%' }} />
            <div className={styles.skeletonLine} />
            <div className={styles.skeletonLine} style={{ width: '85%' }} />
          </div>
        </div>
      </div>
    )
  }

  if (isError) {
    return (
      <>
        <BackLink />
        <StatusMessage title="Couldn't load this event" variant="error" onRetry={() => void refetch()}>
          {error.message}
        </StatusMessage>
      </>
    )
  }

  const cancelled = event.status === 'CANCELLED'

  return (
    <>
      <title>{`${event.title} · Event Ticketing`}</title>
      <BackLink />

      <EventCover id={event.id} title={event.title} muted={cancelled} initialSize="18rem" className={styles.hero}>
        <div className={styles.heroContent}>
          <DateTile iso={event.startTime} size="large" />
          <div className={styles.heroText}>
            <StatusBadge status={event.status} onImage />
            <h1 className={styles.title}>{event.title}</h1>
          </div>
        </div>
      </EventCover>

      <div className={styles.layout}>
        <div>
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>About this event</h2>
            {event.description ? (
              <p className={styles.description}>{event.description}</p>
            ) : (
              <p className={styles.muted}>No description yet.</p>
            )}
          </section>

          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>Date and venue</h2>
            <ul className={styles.facts}>
              <li className={styles.fact}>
                <span className={styles.factIcon}>
                  <Icon name="calendar" size={20} />
                </span>
                <span>
                  <span className={styles.factLabel}>When</span>
                  <time dateTime={event.startTime} className={styles.factValue}>
                    {formatFullDate(event.startTime)}
                  </time>
                  <span className={styles.factSub}> · {formatTime(event.startTime)}</span>
                </span>
              </li>
              <li className={styles.fact}>
                <span className={styles.factIcon}>
                  <Icon name="pin" size={20} />
                </span>
                <span>
                  <span className={styles.factLabel}>Where</span>
                  <span className={styles.factValue}>{event.venue.name}</span>
                  <span className={styles.factSub}> · {event.venue.city}</span>
                </span>
              </li>
            </ul>
          </section>

          <section className={styles.section} id="seating">
            <h2 className={styles.sectionTitle}>{event.status === 'PUBLISHED' ? 'Choose your seats' : 'Seating'}</h2>
            {seatsQuery.isPending ? (
              <div className={styles.skeletonLine} style={{ height: 160 }} />
            ) : seatsQuery.isError ? (
              <p className={styles.muted}>The seating plan couldn't be loaded.</p>
            ) : seatsQuery.data.length > 0 ? (
              event.status === 'PUBLISHED' ? (
                <SeatPicker eventId={event.id} seats={seatsQuery.data} />
              ) : (
                <SeatingPlan seats={seatsQuery.data} />
              )
            ) : (
              <p className={styles.muted}>The seating plan hasn't been published yet.</p>
            )}
          </section>
        </div>

        <aside className={styles.aside} aria-label="Tickets">
          <h2 className={styles.asideTitle}>Tickets</h2>
          <TicketPanel
            event={event}
            seatsState={{ seats: seatsQuery.data, isPending: seatsQuery.isPending, isError: seatsQuery.isError }}
          />
        </aside>
      </div>
    </>
  )
}
