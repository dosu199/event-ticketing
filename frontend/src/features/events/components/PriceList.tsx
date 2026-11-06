import { categoryLabel, formatPrice, lowestPrice, summarizeByCategory } from '../seats'
import type { Seat } from '../types'
import styles from './PriceList.module.css'

export function PriceList({ seats }: { seats: Seat[] }) {
  const from = lowestPrice(seats)
  return (
    <>
      {from !== null && (
        <p className={styles.from}>
          From <strong>{formatPrice(from)}</strong>
        </p>
      )}
      <ul className={styles.list}>
        {summarizeByCategory(seats).map((s) => (
          <li key={s.category} className={styles.item}>
            <span>
              <span className={styles.name}>{categoryLabel[s.category]}</span>
              <span className={styles.count}>{s.count} seats</span>
            </span>
            <span className={styles.price}>
              {s.minPrice === s.maxPrice
                ? formatPrice(s.minPrice)
                : `${formatPrice(s.minPrice)} – ${formatPrice(s.maxPrice)}`}
            </span>
          </li>
        ))}
      </ul>
    </>
  )
}
