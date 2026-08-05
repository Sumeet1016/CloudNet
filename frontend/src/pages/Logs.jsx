import { useEffect, useState } from 'react'
import api from '../api/axios'

const LEVEL_STYLES = {
  INFO: 'bg-gray-100 text-gray-600',
  WARNING: 'bg-yellow-50 text-yellow-600',
  ERROR: 'bg-red-50 text-red-600',
}

export default function Logs() {
  const [logs, setLogs] = useState([])

  useEffect(() => {
    api.get('/logs').then((res) => setLogs(res.data))
  }, [])

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">Activity Logs</h2>
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        {logs.length === 0 ? (
          <p className="text-sm text-gray-400">No activity recorded yet.</p>
        ) : (
          <ul className="divide-y divide-gray-100">
            {logs.map((log) => (
              <li key={log.id} className="py-3 flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm font-medium text-gray-700">{log.action?.replaceAll('_', ' ')}</p>
                  <p className="text-xs text-gray-400">{log.details}</p>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <span className={`px-2 py-0.5 rounded-full text-xs ${LEVEL_STYLES[log.level] || ''}`}>{log.level}</span>
                  <span className="text-xs text-gray-400">{new Date(log.createdAt).toLocaleString()}</span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
