import { useSearchParams } from 'react-router'
import { StatusMessage } from '../components/StatusMessage'
import { EventCard } from '../features/events/components/EventCard'
import { EventCardSkeleton } from '../features/events/components/EventCardSkeleton'
import { statusLabel } from '../features/events/format'
import { useEventsInfinite } from '../features/events/queries'
import type { EventStatus } from '../features/events/types'
import styles from './EventListPage.module.css'

const SKELETON_COUNT = 6
const FILTERS: ReadonlyArray<EventStatus | 'ALL'> = ['ALL', 'PUBLISHED', 'DRAFT', 'CANCELLED']
const countFormat = new Intl.NumberFormat()

function isStatus(value: string | null): value is EventStatus {
  return value === 'PUBLISHED' || value === 'DRAFT' || value === 'CANCELLED'
}

export function EventListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const statusParam = searchParams.get('status')
  const status = isStatus(statusParam) ? statusParam : undefined

  const { data, isPending, isError, error, refetch, fetchNextPage, hasNextPage, isFetchingNextPage, isPlaceholderData } =
    useEventsInfinite({ status })

  const events = data?.pages.flatMap((page) => page.content) ?? []
  const total = data?.pages[0]?.totalElements ?? 0

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
        <div className={styles.titleGroup}>
          <h2 className={styles.sectionTitle}>{status ? statusLabel[status] : 'All events'}</h2>
          {data && <span className={styles.total}>{countFormat.format(total)} events</span>}
        </div>
        <div className={styles.filters} role="group" aria-label="Filter by status">
          {FILTERS.map((filter) => {
            const active = (filter === 'ALL' && !status) || filter === status
            return (
              <button
                key={filter}
                type="button"
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                aria-pressed={active}
                onClick={() => selectFilter(filter)}
              >
                {filter === 'ALL' ? 'All' : statusLabel[filter]}
              </button>
            )
          })}
        </div>
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
      ) : events.length === 0 ? (
        <StatusMessage title={status ? 'No events with this status' : 'No events yet'}>
          {status ? 'Try another status filter.' : 'Check back soon for new events.'}
        </StatusMessage>
      ) : (
        <>
          <ul className={`${styles.grid} ${isPlaceholderData ? styles.stale : ''}`} aria-busy={isPlaceholderData}>
            {events.map((event) => (
              <li key={event.id}>
                <EventCard event={event} />
              </li>
            ))}
          </ul>

          <div className={styles.more}>
            <p className={styles.progress} aria-live="polite">
              Showing {countFormat.format(events.length)} of {countFormat.format(total)}
            </p>
            {hasNextPage && !isPlaceholderData && (
              <button
                type="button"
                className={styles.moreButton}
                onClick={() => void fetchNextPage()}
                disabled={isFetchingNextPage}
              >
                {isFetchingNextPage ? 'Loading…' : 'Load more events'}
              </button>
            )}
          </div>
        </>
      )}
    </>
  )
}
