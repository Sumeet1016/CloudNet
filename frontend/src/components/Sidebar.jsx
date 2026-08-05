import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const links = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/providers', label: 'Cloud Providers' },
  { to: '/policies', label: 'Backup Policies' },
  { to: '/backups', label: 'Backup Jobs' },
  { to: '/schedules', label: 'Schedules' },
  { to: '/logs', label: 'Activity Logs' },
]

export default function Sidebar() {
  const { user, logout } = useAuth()

  return (
    <aside className="w-60 shrink-0 bg-brand-700 text-white min-h-screen flex flex-col">
      <div className="px-5 py-5 border-b border-brand-600">
        <h1 className="text-xl font-bold tracking-tight">CloudNest</h1>
        <p className="text-xs text-brand-100 mt-1">{user?.industry?.replaceAll('_', ' ')}</p>
      </div>

      <nav className="flex-1 px-2 py-4 space-y-1">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            end={link.end}
            className={({ isActive }) =>
              `block px-3 py-2 rounded-md text-sm font-medium transition ${
                isActive ? 'bg-brand-600 text-white' : 'text-brand-100 hover:bg-brand-600/60'
              }`
            }
          >
            {link.label}
          </NavLink>
        ))}
      </nav>

      <div className="px-4 py-4 border-t border-brand-600">
        <p className="text-sm font-medium truncate">{user?.username}</p>
        <p className="text-xs text-brand-100 truncate mb-3">{user?.email}</p>
        <button
          onClick={logout}
          className="w-full text-sm bg-brand-600 hover:bg-brand-500 rounded-md py-1.5 transition"
        >
          Logout
        </button>
      </div>
    </aside>
  )
}
