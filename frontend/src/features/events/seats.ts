import { CURRENCY } from '../../config/env'
import type { Seat, SeatCategory } from './types'

const priceFormat = new Intl.NumberFormat(undefined, { style: 'currency', currency: CURRENCY })

export function formatPrice(value: number): string {
  return priceFormat.format(value)
}

export const categoryLabel: Record<SeatCategory, string> = {
  VIP: 'VIP',
  STANDARD: 'Standard',
}

export interface CategorySummary {
  category: SeatCategory
  count: number
  minPrice: number
  maxPrice: number
}

const CATEGORY_ORDER: SeatCategory[] = ['VIP', 'STANDARD']

export function summarizeByCategory(seats: Seat[]): CategorySummary[] {
  const byCategory = new Map<SeatCategory, CategorySummary>()
  for (const seat of seats) {
    const current = byCategory.get(seat.category)
    if (current) {
      current.count += 1
      current.minPrice = Math.min(current.minPrice, seat.price)
      current.maxPrice = Math.max(current.maxPrice, seat.price)
    } else {
      byCategory.set(seat.category, { category: seat.category, count: 1, minPrice: seat.price, maxPrice: seat.price })
    }
  }
  return CATEGORY_ORDER.flatMap((c) => byCategory.get(c) ?? [])
}

export function groupByRow(seats: Seat[]): Array<{ row: string; seats: Seat[] }> {
  const rows = new Map<string, Seat[]>()
  for (const seat of seats) {
    const row = rows.get(seat.rowLabel) ?? []
    row.push(seat)
    rows.set(seat.rowLabel, row)
  }
  return [...rows].map(([row, rowSeats]) => ({ row, seats: rowSeats }))
}

export function lowestPrice(seats: Seat[]): number | null {
  return seats.length === 0 ? null : Math.min(...seats.map((s) => s.price))
}
