import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const links = [
  ['/dashboard', 'Dashboard'],
  ['/subscriptions', 'Subscriptions'],
  ['/merchants', 'Merchants'],
  ['/payments', 'Payments'],
  ['/calendar', 'Renewal Calendar'],
  ['/reports', 'Reports'],
  ['/notifications', 'Notifications'],
  ['/settings', 'Settings'],
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><span className="brand-mark">O</span> OneSub</div>
        <nav>
          {links.map(([to, label]) => (
            <NavLink key={to} to={to} className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
              {label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="topbar-title">Subscription Management</div>
          <div className="topbar-actions">
            <span>{user?.fullName}</span>
            <button className="button ghost" onClick={() => void handleLogout()}>Logout</button>
          </div>
        </header>
        <main className="content"><Outlet /></main>
      </div>
    </div>
  )
}
