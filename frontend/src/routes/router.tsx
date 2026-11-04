import { createBrowserRouter } from 'react-router'
import { Layout } from '../components/Layout'
import { RouteErrorPage } from '../components/RouteErrorPage'
import { EventDetailPage } from '../pages/EventDetailPage'
import { EventListPage } from '../pages/EventListPage'
import { NotFoundPage } from '../pages/NotFoundPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    errorElement: <RouteErrorPage />,
    children: [
      { index: true, element: <EventListPage /> },
      { path: 'events/:eventId', element: <EventDetailPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
