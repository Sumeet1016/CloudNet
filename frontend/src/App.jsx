import { Routes, Route } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import CloudProviders from './pages/CloudProviders'
import Policies from './pages/Policies'
import BackupJobs from './pages/BackupJobs'
import Schedules from './pages/Schedules'
import Logs from './pages/Logs'
import ProtectedRoute from './components/ProtectedRoute'
import Sidebar from './components/Sidebar'

function Layout({ children }) {
  return (
    <div className="flex">
      <Sidebar />
      <main className="flex-1 p-8">{children}</main>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route path="/" element={
        <ProtectedRoute><Layout><Dashboard /></Layout></ProtectedRoute>
      } />
      <Route path="/providers" element={
        <ProtectedRoute><Layout><CloudProviders /></Layout></ProtectedRoute>
      } />
      <Route path="/policies" element={
        <ProtectedRoute><Layout><Policies /></Layout></ProtectedRoute>
      } />
      <Route path="/backups" element={
        <ProtectedRoute><Layout><BackupJobs /></Layout></ProtectedRoute>
      } />
      <Route path="/schedules" element={
        <ProtectedRoute><Layout><Schedules /></Layout></ProtectedRoute>
      } />
      <Route path="/logs" element={
        <ProtectedRoute><Layout><Logs /></Layout></ProtectedRoute>
      } />
    </Routes>
  )
}
