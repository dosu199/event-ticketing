import { useEffect, useState } from 'react'
import { ApiError } from '../../../api/http'
import { Icon } from '../../../components/Icon'
import { formatCountdown, useCountdown } from '../../../hooks/useCountdown'
import { categoryLabel, formatPrice, groupByRow } from '../../events/seats'
import type { Seat } from '../../events/types'
import { HOLD_URGENT_MS, MAX_SEATS_PER_HOLD } from '../config'
import { useAvailability, useCreateHold, useReleaseHold } from '../queries'
import styles from './SeatPicker.module.css'

type SeatState = 'available' | 'selected' | 'mine' | 'taken'

const stateLabel: Record<SeatState, string> = {
  available: 'available',
  selected: 'selected',
  mine: 'reserved for you',
  taken: 'taken',
}

const seatName = (seat: Seat) => `${seat.rowLabel}${seat.seatNumber}`

function total(seats: Seat[]): number {
  // Sum in cents to avoid floating-point drift (0.1 + 0.2 !== 0.3).
  return seats.reduce((cents, s) => cents + Math.round(s.price * 100), 0) / 100
}

function conflictSeatIds(error: unknown): number[] {
  if (!(error instanceof ApiError) || !error.isConflict) return []
  const ids = error.problem?.unavailableSeatIds
  return Array.isArray(ids) ? ids.filter((id): id is number => typeof id === 'number') : []
}

interface SeatPickerProps {
  eventId: number
  seats: Seat[]
}

export function SeatPicker({ eventId, seats }: SeatPickerProps) {
  const availability = useAvailability(eventId, true)
  const createHold = useCreateHold(eventId)
  const releaseHold = useReleaseHold(eventId)
  const [selection, setSelection] = useState<number[]>([])
  const [limitHit, setLimitHit] = useState(false)

  const myHold = availability.data?.myHold ?? null
  const remaining = useCountdown(myHold?.expiresAt)
  const holdExpired = myHold !== null && remaining === 0

  // When the countdown reaches zero, Redis has already released the seats: refresh.
  const { refetch } = availability
  useEffect(() => {
    if (holdExpired) void refetch()
  }, [holdExpired, refetch])

  const taken = new Set(availability.data?.unavailableSeatIds ?? [])
  const mine = new Set(myHold?.seatIds ?? [])
  const byId = new Map(seats.map((s) => [s.id, s]))

  // Selected seats that someone else grabbed in the meantime (seen on the next poll).
  const lostSeats = selection.filter((id) => taken.has(id))
  const activeSelection = selection.filter((id) => !taken.has(id))

  const stateOf = (seat: Seat): SeatState => {
    if (mine.has(seat.id)) return 'mine'
    if (taken.has(seat.id)) return 'taken'
    if (activeSelection.includes(seat.id)) return 'selected'
    return 'available'
  }

  const toggle = (seatId: number) => {
    createHold.reset()
    if (selection.includes(seatId)) {
      setLimitHit(false)
      setSelection(selection.filter((id) => id !== seatId))
    } else if (activeSelection.length >= MAX_SEATS_PER_HOLD) {
      setLimitHit(true)
    } else {
      setSelection([...activeSelection, seatId])
    }
  }

  const reserve = () => {
    createHold.mutate(activeSelection, {
      onSuccess: () => setSelection([]),
      onError: (error) => {
        // Keep the seats that are still free selected; drop the ones that were taken.
        const lost = new Set(conflictSeatIds(error))
        setSelection((current) => current.filter((id) => !lost.has(id)))
      },
    })
  }

  const release = () => {
    if (myHold) releaseHold.mutate(myHold.holdId)
  }

  if (availability.isPending) {
    return <div className={styles.picker} aria-busy="true">Loading seat availability…</div>
  }

  const bookingDown = availability.isError
  const rows = groupByRow(seats)
  const heldSeats = (myHold?.seatIds ?? []).flatMap((id) => byId.get(id) ?? [])
  const selectedSeats = activeSelection.flatMap((id) => byId.get(id) ?? [])
  const conflictNames = conflictSeatIds(createHold.error).flatMap((id) => {
    const seat = byId.get(id)
    return seat ? [seatName(seat)] : []
  })

  return (
    <div className={styles.picker}>
      {bookingDown && (
        <p className={`${styles.message} ${styles.info}`} role="status">
          <Icon name="alert" size={18} />
          Seat selection is temporarily unavailable. You can still see the seating plan and prices.
        </p>
      )}

      <div className={styles.plan}>
        <div className={styles.stage}>Stage</div>
        <ol className={styles.rows} aria-label="Seats">
          {rows.map(({ row, seats: rowSeats }) => (
            <li key={row} className={styles.row}>
              <span className={styles.rowLabel} aria-hidden="true">
                {row}
              </span>
              <span className={styles.seats}>
                {rowSeats.map((seat) => {
                  const state = stateOf(seat)
                  const disabled = bookingDown || myHold !== null || state === 'taken' || createHold.isPending
                  return (
                    <button
                      key={seat.id}
                      type="button"
                      className={`${styles.seat} ${styles[state]} ${styles[seat.category]}`}
                      aria-pressed={state === 'selected'}
                      aria-label={`Row ${seat.rowLabel}, seat ${seat.seatNumber}, ${categoryLabel[seat.category]}, ${formatPrice(seat.price)}, ${stateLabel[state]}`}
                      title={`${seatName(seat)} · ${categoryLabel[seat.category]} · ${formatPrice(seat.price)}`}
                      disabled={disabled}
                      onClick={() => toggle(seat.id)}
                    >
                      {seat.seatNumber}
                    </button>
                  )
                })}
              </span>
            </li>
          ))}
        </ol>

        <ul className={styles.legend}>
          <li className={styles.legendItem}>
            <span className={`${styles.seat} ${styles.available} ${styles.VIP}`} aria-hidden="true" />
            VIP
          </li>
          <li className={styles.legendItem}>
            <span className={`${styles.seat} ${styles.available} ${styles.STANDARD}`} aria-hidden="true" />
            Standard
          </li>
          <li className={styles.legendItem}>
            <span className={`${styles.seat} ${styles.selected}`} aria-hidden="true" />
            Selected
          </li>
          <li className={styles.legendItem}>
            <span className={`${styles.seat} ${styles.mine}`} aria-hidden="true" />
            Reserved for you
          </li>
          <li className={styles.legendItem}>
            <span className={`${styles.seat} ${styles.taken}`} aria-hidden="true" />
            Taken
          </li>
        </ul>
      </div>

      <div className={styles.summary} aria-live="polite">
        {myHold && !holdExpired ? (
          <>
            <div className={styles.summaryHeader}>
              <h3 className={styles.summaryTitle}>Seats reserved for you</h3>
              <span
                className={`${styles.timer} ${remaining < HOLD_URGENT_MS ? styles.timerUrgent : ''}`}
                aria-label={`Reservation expires in ${formatCountdown(remaining)}`}
              >
                <Icon name="clock" size={16} />
                {formatCountdown(remaining)}
              </span>
            </div>
            <ul className={styles.chips}>
              {heldSeats.map((seat) => (
                <li key={seat.id} className={`${styles.chip} ${styles.chipStatic}`}>
                  {seatName(seat)} · {categoryLabel[seat.category]} · {formatPrice(seat.price)}
                </li>
              ))}
            </ul>
            <p className={styles.total}>
              Total <strong>{formatPrice(total(heldSeats))}</strong>
            </p>
            <div className={styles.actions}>
              <button type="button" className={styles.secondary} onClick={release} disabled={releaseHold.isPending}>
                {releaseHold.isPending ? 'Releasing…' : 'Release seats'}
              </button>
              <button type="button" className={styles.primary} disabled title="Payment comes in the next step">
                Continue to payment
              </button>
            </div>
            <p className={`${styles.message} ${styles.info}`}>
              <Icon name="clock" size={18} />
              The seats are yours until the timer runs out. Payment is coming in the next step of the project.
            </p>
          </>
        ) : (
          <>
            <div className={styles.summaryHeader}>
              <h3 className={styles.summaryTitle}>
                {selectedSeats.length === 0 ? 'Pick your seats' : `${selectedSeats.length} selected`}
              </h3>
              <span className={styles.hint}>Up to {MAX_SEATS_PER_HOLD} seats</span>
            </div>

            {holdExpired && (
              <p className={`${styles.message} ${styles.info}`} role="status">
                <Icon name="clock" size={18} />
                Your reservation expired and the seats were released.
              </p>
            )}
            {lostSeats.length > 0 && (
              <p className={`${styles.message} ${styles.error}`} role="alert">
                <Icon name="alert" size={18} />
                {lostSeats.flatMap((id) => (byId.get(id) ? [seatName(byId.get(id)!)] : [])).join(', ')} just got
                taken by someone else.
              </p>
            )}
            {conflictNames.length > 0 && (
              <p className={`${styles.message} ${styles.error}`} role="alert">
                <Icon name="alert" size={18} />
                {conflictNames.join(', ')} {conflictNames.length === 1 ? 'was' : 'were'} taken a moment ago. Your other
                seats are still selected.
              </p>
            )}
            {createHold.isError && conflictNames.length === 0 && (
              <p className={`${styles.message} ${styles.error}`} role="alert">
                <Icon name="alert" size={18} />
                {createHold.error.message}
              </p>
            )}
            {limitHit && (
              <p className={`${styles.message} ${styles.info}`} role="status">
                <Icon name="alert" size={18} />
                You can reserve at most {MAX_SEATS_PER_HOLD} seats at a time.
              </p>
            )}

            {selectedSeats.length > 0 ? (
              <ul className={styles.chips}>
                {selectedSeats.map((seat) => (
                  <li key={seat.id} className={styles.chip}>
                    {seatName(seat)} · {formatPrice(seat.price)}
                    <button
                      type="button"
                      className={styles.chipRemove}
                      aria-label={`Remove ${seatName(seat)}`}
                      onClick={() => toggle(seat.id)}
                    >
                      ×
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              !bookingDown && <p className={styles.hint}>Click free seats on the plan to select them.</p>
            )}

            <div className={styles.actions}>
              <p className={styles.total}>
                Total <strong>{formatPrice(total(selectedSeats))}</strong>
              </p>
              <button
                type="button"
                className={styles.primary}
                onClick={reserve}
                disabled={selectedSeats.length === 0 || createHold.isPending || bookingDown}
              >
                {createHold.isPending
                  ? 'Reserving…'
                  : selectedSeats.length === 0
                    ? 'Reserve seats'
                    : `Reserve ${selectedSeats.length} seat${selectedSeats.length === 1 ? '' : 's'}`}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
