import { isRouteErrorResponse, useRouteError } from 'react-router'
import { StatusMessage } from './StatusMessage'

export function RouteErrorPage() {
  const error = useRouteError()
  const message = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : error instanceof Error
      ? error.message
      : 'Unknown error'

  return (
    <div style={{ maxWidth: 640, margin: '64px auto', padding: '0 20px' }}>
      <title>Something went wrong · Event Ticketing</title>
      <StatusMessage title="Something went wrong" variant="error" backTo={{ to: '/', label: 'All events' }}>
        {message}
      </StatusMessage>
    </div>
  )
}
