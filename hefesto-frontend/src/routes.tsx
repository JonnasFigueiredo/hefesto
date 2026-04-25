import { Navigate, Route, Routes } from 'react-router-dom'
import { ChatPage } from '@/pages/ChatPage'
import { JiraPage } from '@/pages/JiraPage'
import { SettingsPage } from '@/pages/SettingsPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/chat" replace />} />
      <Route path="/chat" element={<ChatPage />} />
      <Route path="/jira" element={<JiraPage />} />
      <Route path="/settings" element={<SettingsPage />} />
      <Route path="*" element={<Navigate to="/chat" replace />} />
    </Routes>
  )
}
