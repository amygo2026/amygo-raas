import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import HomePage from './pages/HomePage'
import OpsConsole from './pages/OpsConsole'
import CommandCenterPage from './pages/CommandCenterPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/ops" element={<OpsConsole />} />
        <Route path="/demo/command-center" element={<CommandCenterPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
