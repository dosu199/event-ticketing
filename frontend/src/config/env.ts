const DEFAULT_API_BASE_URL = 'http://localhost:8080'

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? DEFAULT_API_BASE_URL).replace(/\/+$/, '')

// Prices from the API have no currency yet; all demo prices are in euros.
// When the backend adds a currency field, read it from there instead.
export const CURRENCY = 'EUR'
