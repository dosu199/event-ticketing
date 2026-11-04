import { StatusMessage } from '../components/StatusMessage'

export function NotFoundPage() {
  return (
    <>
      <title>Page not found · Event Ticketing</title>
      <StatusMessage title="Page not found" backTo={{ to: '/', label: 'Browse events' }}>
        There's nothing at this address. It may have moved, or the link may be wrong.
      </StatusMessage>
    </>
  )
}
