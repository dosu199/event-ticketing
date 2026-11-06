import { categoryLabel, formatPrice, groupByRow, summarizeByCategory } from '../seats'
import type { Seat } from '../types'
import styles from './SeatingPlan.module.css'

export function SeatingPlan({ seats }: { seats: Seat[] }) {
  const rows = groupByRow(seats)
  const summary = summarizeByCategory(seats)

  return (
    <figure className={styles.wrapper} aria-label={`Seating plan, ${seats.length} seats`}>
      <div className={styles.stage}>Stage</div>
      <ol className={styles.rows}>
        {rows.map(({ row, seats: rowSeats }) => (
          <li key={row} className={styles.row}>
            <span className={styles.rowLabel} aria-hidden="true">
              {row}
            </span>
            <span className={styles.seats}>
              {rowSeats.map((seat) => (
                <span
                  key={seat.id}
                  className={`${styles.seat} ${styles[seat.category]}`}
                  title={`Row ${seat.rowLabel}, seat ${seat.seatNumber} · ${categoryLabel[seat.category]} · ${formatPrice(seat.price)}`}
                />
              ))}
            </span>
          </li>
        ))}
      </ol>
      <ul className={styles.legend}>
        {summary.map((s) => (
          <li key={s.category} className={styles.legendItem}>
            <span className={`${styles.seat} ${styles[s.category]}`} aria-hidden="true" />
            {categoryLabel[s.category]}
          </li>
        ))}
      </ul>
    </figure>
  )
}
