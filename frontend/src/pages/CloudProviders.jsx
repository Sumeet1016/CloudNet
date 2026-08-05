import { useEffect, useState } from 'react'
import api from '../api/axios'

const PROVIDER_TYPES = [
  { value: 'LOCAL_DISK', label: 'Local Disk', needsToken: false },
  { value: 'SIMULATED_S3', label: 'Simulated S3 Bucket', needsToken: false },
  { value: 'GOOGLE_DRIVE', label: 'Google Drive', needsToken: true },
]

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

export default function CloudProviders() {
  const [providers, setProviders] = useState([])
  const [usage, setUsage] = useState({})
  const [type, setType] = useState('LOCAL_DISK')
  const [displayName, setDisplayName] = useState('')
  const [authCode, setAuthCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = () => {
    api.get('/cloud-providers').then((res) => {
      setProviders(res.data)
      res.data.forEach((p) => {
        api.get(`/cloud-providers/${p.id}/usage`).then((u) =>
          setUsage((prev) => ({ ...prev, [p.id]: u.data }))
        )
      })
    })
  }

  useEffect(load, [])

  const connect = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await api.post('/cloud-providers/connect', {
        type,
        displayName: displayName || undefined,
        authorizationCode: type === 'GOOGLE_DRIVE' ? authCode : undefined,
      })
      setDisplayName('')
      setAuthCode('')
      load()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to connect provider')
    } finally {
      setLoading(false)
    }
  }

  const disconnect = async (id) => {
    await api.delete(`/cloud-providers/${id}`)
    load()
  }

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">Cloud Providers</h2>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Connect a New Provider</h3>
        {error && <div className="mb-3 text-sm bg-red-50 text-red-600 border border-red-100 rounded-md px-3 py-2">{error}</div>}
        <form onSubmit={connect} className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Provider Type</label>
            <select className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" value={type} onChange={(e) => setType(e.target.value)}>
              {PROVIDER_TYPES.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Display Name (optional)</label>
            <input className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          </div>
          {type === 'GOOGLE_DRIVE' && (
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Google OAuth Access Token</label>
              <input className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" value={authCode} onChange={(e) => setAuthCode(e.target.value)} placeholder="Paste access token" />
            </div>
          )}
          <button disabled={loading} className="bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium py-2 rounded-md transition disabled:opacity-60">
            {loading ? 'Connecting...' : 'Connect'}
          </button>
        </form>
        {type === 'GOOGLE_DRIVE' && (
          <p className="text-xs text-gray-400 mt-2">
            For a class demo: generate a token via Google's OAuth Playground with the drive.file scope and paste it above.
            See the README for setting up real OAuth credentials in application.yml.
          </p>
        )}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Connected Providers</h3>
        {providers.length === 0 ? (
          <p className="text-sm text-gray-400">No providers connected yet.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500 border-b border-gray-100">
                <th className="pb-2">Name</th>
                <th className="pb-2">Type</th>
                <th className="pb-2">Usage</th>
                <th className="pb-2">Status</th>
                <th className="pb-2"></th>
              </tr>
            </thead>
            <tbody>
              {providers.map((p) => (
                <tr key={p.id} className="border-b border-gray-50">
                  <td className="py-2">{p.displayName}</td>
                  <td className="py-2">{p.type.replaceAll('_', ' ')}</td>
                  <td className="py-2">{formatBytes(usage[p.id])}</td>
                  <td className="py-2">
                    <span className={`px-2 py-0.5 rounded-full text-xs ${p.connected ? 'bg-green-50 text-green-600' : 'bg-gray-100 text-gray-500'}`}>
                      {p.connected ? 'Connected' : 'Disconnected'}
                    </span>
                  </td>
                  <td className="py-2 text-right">
                    {p.connected && (
                      <button onClick={() => disconnect(p.id)} className="text-red-500 text-xs hover:underline">Disconnect</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
