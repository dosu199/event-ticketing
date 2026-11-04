import type { EventStatus } from './types'

const fullDate = new Intl.DateTimeFormat(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
const time = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' })
const cardDate = new Intl.DateTimeFormat(undefined, { weekday: 'short', day: 'numeric', month: 'short' })
const monthShort = new Intl.DateTimeFormat(undefined, { month: 'short' })
const dayOfMonth = new Intl.DateTimeFormat(undefined, { day: 'numeric' })

export function formatFullDate(iso: string): string {
  return fullDate.format(new Date(iso))
}

export function formatTime(iso: string): string {
  return time.format(new Date(iso))
}

export function formatCardDate(iso: string): string {
  return `${cardDate.format(new Date(iso))} · ${time.format(new Date(iso))}`
}

export function dateTileParts(iso: string): { month: string; day: string } {
  const date = new Date(iso)
  return { month: monthShort.format(date).replace('.', ''), day: dayOfMonth.format(date) }
}

export const statusLabel: Record<EventStatus, string> = {
  DRAFT: 'Draft',
  PUBLISHED: 'On sale',
  CANCELLED: 'Cancelled',
}

export function parseEventId(raw: string | undefined): number | null {
  if (!raw || !/^\d+$/.test(raw)) return null
  const id = Number(raw)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}
