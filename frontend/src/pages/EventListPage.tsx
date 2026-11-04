import { useSearchParams } from 'react-router'
import { StatusMessage } from '../components/StatusMessage'
import { EventCard } from '../features/events/components/EventCard'
import { EventCardSkeleton } from '../features/events/components/EventCardSkeleton'
import { statusLabel } from '../features/events/format'
import { useEvents } from '../features/events/queries'
import type { EventStatus } from '../features/events/types'
import styles from './EventListPage.module.css'

const SKELETON_COUNT = 3
const FILTERS: ReadonlyArray<EventStatus | 'ALL'> = ['ALL', 'PUBLISHED', 'DRAFT', 'CANCELLED']

function isFilter(value: string | null): value is EventStatus {
  return value === 'PUBLISHED' || value === 'DRAFT' || value === 'CANCELLED'
}

export function EventListPage() {
  const { data: events, isPending, isError, error, refetch } = useEvents()
  const [searchParams, setSearchParams] = useSearchParams()
  const statusParam = searchParams.get('status')
  const activeFilter: EventStatus | 'ALL' = isFilter(statusParam) ? statusParam : 'ALL'

  const visible = events?.filter((e) => activeFilter === 'ALL' || e.status === activeFilter) ?? []
  const countFor = (filter: EventStatus | 'ALL') =>
    filter === 'ALL' ? (events?.length ?? 0) : (events?.filter((e) => e.status === filter).length ?? 0)

  const selectFilter = (filter: EventStatus | 'ALL') => {
    setSearchParams(filter === 'ALL' ? {} : { status: filter }, { replace: true })
  }

  return (
    <>
      <title>Events · Event Ticketing</title>

      <section className={styles.hero}>
        <span className={styles.eyebrow}>Live events</span>
        <h1 className={styles.heading}>Find your next night out</h1>
        <p className={styles.subheading}>
          Concerts, conferences and shows near you. Pick an event to see the details and grab your seats.
        </p>
      </section>

      <div className={styles.toolbar}>
        <h2 className={styles.sectionTitle}>All events</h2>
        {events && events.length > 0 && (
          <div className={styles.filters} role="group" aria-label="Filter by status">
            {FILTERS.map((filter) => {
              const active = filter === activeFilter
              return (
                <button
                  key={filter}
                  type="button"
                  className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                  aria-pressed={active}
                  onClick={() => selectFilter(filter)}
                >
                  {filter === 'ALL' ? 'All' : statusLabel[filter]}
                  <span className={styles.count}>{countFor(filter)}</span>
                </button>
              )
            })}
          </div>
        )}
      </div>

      {isPending ? (
        <ul className={styles.grid} aria-busy="true" aria-label="Loading events">
          {Array.from({ length: SKELETON_COUNT }, (_, i) => (
            <li key={i}>
              <EventCardSkeleton />
            </li>
          ))}
        </ul>
      ) : isError ? (
        <StatusMessage title="Couldn't load events" variant="error" onRetry={() => void refetch()}>
          {error.message}
        </StatusMessage>
      ) : visible.length === 0 ? (
        <StatusMessage title={events.length === 0 ? 'No events yet' : 'No events match this filter'}>
          {events.length === 0 ? 'Check back soon for new events.' : 'Try another status filter.'}
        </StatusMessage>
      ) : (
        <ul className={styles.grid}>
          {visible.map((event) => (
            <li key={event.id}>
              <EventCard event={event} />
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
