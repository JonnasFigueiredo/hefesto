import { NavLink } from 'react-router-dom'
import { MessageSquare, GitBranch, BarChart3 } from 'lucide-react'
import { cn } from '@/lib/cn'
import { SystemStatus } from './SystemStatus'

interface NavItem {
  to: string
  label: string
  icon: typeof MessageSquare
}

// Settings removido da nav do sidebar — uso interno apenas.
// Rota /settings continua acessível direto pela URL pra debug/admin.
const NAV: NavItem[] = [
  { to: '/chat', label: 'CHAT', icon: MessageSquare },
  { to: '/jira', label: 'JIRA', icon: GitBranch },
  { to: '/analytics', label: 'ANALYTICS', icon: BarChart3 },
]

export function Sidebar() {
  return (
    <aside className="w-[240px] shrink-0 h-full border-r border-[var(--border-dim)] flex flex-col bg-[var(--bg-base)]">
      {/* Logo */}
      <div className="px-5 py-6 border-b border-[var(--border-dim)]">
        <div className="font-display text-[22px] font-semibold uppercase tracking-[0.3em] text-[var(--accent-cyan)] glow-cyan">
          [ HEFESTO ]
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 py-4">
        {NAV.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              cn(
                'group flex items-center gap-3 h-11 px-5 relative',
                'font-display uppercase tracking-[0.18em] text-[13px] transition-colors',
                isActive
                  ? 'text-[var(--accent-cyan)] bg-[var(--bg-elevated)]'
                  : 'text-[var(--text-dim)] hover:text-[var(--text)]',
              )
            }
          >
            {({ isActive }) => (
              <>
                {/* Active marker */}
                <span
                  aria-hidden
                  className={cn(
                    'absolute left-0 top-0 bottom-0 w-[2px] transition-colors',
                    isActive ? 'bg-[var(--accent-cyan)]' : 'bg-transparent',
                  )}
                />
                <item.icon size={16} strokeWidth={1.5} />
                <span>{item.label}</span>
                <span
                  aria-hidden
                  className={cn(
                    'ml-auto h-3 w-[2px] transition-colors',
                    isActive ? 'bg-[var(--accent-cyan)]' : 'bg-[var(--border-dim)]',
                  )}
                />
              </>
            )}
          </NavLink>
        ))}
      </nav>

      {/* Status footer */}
      <SystemStatus />
    </aside>
  )
}
