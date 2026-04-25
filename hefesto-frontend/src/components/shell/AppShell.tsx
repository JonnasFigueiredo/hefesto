import type { ReactNode } from 'react'
import { Sidebar } from './Sidebar'
import { TopBar } from './TopBar'
import { GridBackground } from '@/components/ui/GridBackground'
import { ScanlineOverlay } from '@/components/ui/ScanlineOverlay'

interface AppShellProps {
  children: ReactNode
}

export function AppShell({ children }: AppShellProps) {
  return (
    <div className="h-screen w-screen flex bg-[var(--bg-base)] overflow-hidden">
      <GridBackground />
      <ScanlineOverlay enabled={false} />

      <Sidebar />

      <div className="flex-1 flex flex-col min-w-0">
        <TopBar />
        <main className="flex-1 overflow-auto p-6">{children}</main>
      </div>
    </div>
  )
}
