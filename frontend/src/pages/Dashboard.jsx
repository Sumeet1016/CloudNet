import { useEffect, useState } from 'react'
import api from '../api/axios'
import StatCard from '../components/StatCard'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'

function formatBytes(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let val = bytes
  while (val >= 1024 && i < units.length - 1) {
    val /= 1024
    i++
  }
  return `${val.toFixed(1)} ${units[i]}`
}

export default function Dashboard() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/dashboard/stats')
      .then((res) => setStats(res.data))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="text-gray-500">Loading dashboard...</p>
  if (!stats) return <p className="text-gray-500">No data available yet.</p>

  const chartData = Object.entries(stats.backupsByProvider || {}).map(([name, count]) => ({ name, count }))

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">Dashboard</h2>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard label="Total Backups" value={stats.totalBackups} />
        <StatCard label="Successful" value={stats.successfulBackups} accent="text-green-600" />
        <StatCard label="Failed" value={stats.failedBackups} accent="text-red-600" />
        <StatCard label="Storage Used" value={formatBytes(stats.totalStorageUsedBytes)} />
        <StatCard label="Connected Providers" value={stats.connectedProviders} />
        <StatCard label="Active Schedules" value={stats.activeSchedules} />
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Backups by Provider</h3>
        {chartData.length === 0 ? (
          <p className="text-sm text-gray-400">No backups yet. Run your first backup to see stats here.</p>
        ) : (
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" fontSize={12} />
              <YAxis allowDecimals={false} fontSize={12} />
              <Tooltip />
              <Bar dataKey="count" fill="#3b6fe0" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Recent Activity</h3>
        {(!stats.recentActivity || stats.recentActivity.length === 0) ? (
          <p className="text-sm text-gray-400">No activity yet.</p>
        ) : (
          <ul className="divide-y divide-gray-100">
            {stats.recentActivity.map((log, idx) => (
              <li key={idx} className="py-2 text-sm flex justify-between">
                <span className="text-gray-700">{log.action?.replaceAll('_', ' ')}</span>
                <span className="text-gray-400">{log.details}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
