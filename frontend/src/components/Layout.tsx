import { Link, Outlet, ScrollRestoration, useLocation } from 'react-router'
import { Icon } from './Icon'
import styles from './Layout.module.css'

export function Layout() {
  const { pathname } = useLocation()
  const onEvents = pathname === '/' || pathname.startsWith('/events')

  return (
    <>
      <header className={styles.header}>
        <div className={`${styles.container} ${styles.headerInner}`}>
          <Link to="/" className={styles.brand}>
            <span className={styles.logo}>
              <Icon name="ticket" size={18} />
            </span>
            Event Ticketing
          </Link>
          <nav className={styles.nav} aria-label="Main">
            <Link
              to="/"
              className={`${styles.navLink} ${onEvents ? styles.navLinkActive : ''}`}
              aria-current={pathname === '/' ? 'page' : undefined}
            >
              Events
            </Link>
          </nav>
        </div>
      </header>

      <main className={`${styles.container} ${styles.main}`}>
        <Outlet />
      </main>

      <footer className={styles.footer}>
        <div className={`${styles.container} ${styles.footerInner}`}>
          <span>Event Ticketing</span>
          <span>A microservices learning project</span>
        </div>
      </footer>
      <ScrollRestoration />
    </>
  )
}
