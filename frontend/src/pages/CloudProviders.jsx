import { useEffect, useState } from 'react'
import api from '../api/axios'

const PROVIDER_TYPES = [
  { value: 'LOCAL_DISK', label: 'Local Disk', needsToken: false },
  { value: 'SIMULATED_S3', label: 'Simulated S3 Bucket', needsToken: false },
  { value: 'GOOGLE_DRIVE', label: 'Google Drive', needsToken: true },
  { value: 'BACKBLAZE_B2', label: 'Backblaze B2 Bucket', needsToken: true },
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
  const [quotas, setQuotas] = useState({})
  const [health, setHealth] = useState({})
  const [type, setType] = useState('LOCAL_DISK')
  const [displayName, setDisplayName] = useState('')
  const [authCode, setAuthCode] = useState('')
  const [accessKeyId, setAccessKeyId] = useState('')
  const [bucketName, setBucketName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = () => {
    api.get('/cloud-providers').then((res) => {
      setProviders(res.data)
      res.data.forEach((p) => {
        api.get(`/cloud-providers/${p.id}/usage`).then((u) =>
          setUsage((prev) => ({ ...prev, [p.id]: u.data }))
        )
        api.get(`/cloud-providers/${p.id}/quota`).then((q) =>
          setQuotas((prev) => ({ ...prev, [p.id]: q.data }))
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
        authorizationCode:
          type === 'GOOGLE_DRIVE' || type === 'BACKBLAZE_B2' ? authCode : undefined,
        accessKeyId: type === 'BACKBLAZE_B2' ? accessKeyId : undefined,
        bucketName: type === 'BACKBLAZE_B2' ? bucketName || undefined : undefined,
      })
      setDisplayName('')
      setAuthCode('')
      setAccessKeyId('')
      setBucketName('')
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

  const runHealthCheck = async (id) => {
    const res = await api.get(`/cloud-providers/${id}/health`)
    setHealth((prev) => ({ ...prev, [id]: res.data }))
  }

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">Cloud Providers</h2>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Connect a New Provider</h3>
        {error && (
          <div className="mb-3 text-sm bg-red-50 text-red-600 border border-red-100 rounded-md px-3 py-2">
            {error}
          </div>
        )}
        <form onSubmit={connect} className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Provider Type</label>
            <select
              className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm"
              value={type}
              onChange={(e) => setType(e.target.value)}
            >
              {PROVIDER_TYPES.map((p) => (
                <option key={p.value} value={p.value}>
                  {p.label}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">
              Display Name (optional)
            </label>
            <input
              className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </div>

          {type === 'GOOGLE_DRIVE' && (
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">
                Google OAuth Access Token
              </label>
              <input
                className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm"
                value={authCode}
                onChange={(e) => setAuthCode(e.target.value)}
                placeholder="Paste access token"
              />
            </div>
          )}

          {type === 'BACKBLAZE_B2' && (
            <>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Key ID</label>
                <input
                  className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm"
                  value={accessKeyId}
                  onChange={(e) => setAccessKeyId(e.target.value)}
                  placeholder="Backblaze application key ID"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Application Key</label>
                <input
                  type="password"
                  className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm"
                  value={authCode}
                  onChange={(e) => setAuthCode(e.target.value)}
                  placeholder="Backblaze application key"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">
                  Bucket Name (optional)
                </label>
                <input
                  className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm"
                  value={bucketName}
                  onChange={(e) => setBucketName(e.target.value)}
                  placeholder="cloudnest-backups"
                />
              </div>
            </>
          )}

          <button
            disabled={loading}
            className="bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium py-2 rounded-md transition disabled:opacity-60"
          >
            {loading ? 'Connecting...' : 'Connect'}
          </button>
        </form>

        {type === 'GOOGLE_DRIVE' && (
          <p className="text-xs text-gray-400 mt-2">
            For a class demo: generate a token via Google's OAuth Playground with the drive.file scope
            and paste it above.
          </p>
        )}
        {type === 'BACKBLAZE_B2' && (
          <p className="text-xs text-gray-400 mt-2">
            Get the access key id and secret key from your Upstash Blob bucket page. Bucket name is
            optional — leave blank to use the configured default.
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
                <th className="pb-2">Usage / Quota</th>
                <th className="pb-2">Status</th>
                <th className="pb-2"></th>
              </tr>
            </thead>
            <tbody>
              {providers.map((p) => {
                const q = quotas[p.id]
                const h = health[p.id]
                return (
                  <tr key={p.id} className="border-b border-gray-50">
                    <td className="py-2">{p.displayName}</td>
                    <td className="py-2">{p.type.replaceAll('_', ' ')}</td>
                    <td className="py-2 w-56">
                      <div className="text-xs text-gray-600 mb-1">
                        {formatBytes(usage[p.id])} / {q ? formatBytes(q.quotaBytes) : '—'}
                      </div>
                      {q && (
                        <div className="w-full bg-gray-100 rounded-full h-1.5">
                          <div
                            className={`h-1.5 rounded-full ${
                              q.percentUsed >= 95
                                ? 'bg-red-500'
                                : q.percentUsed >= 80
                                ? 'bg-amber-500'
                                : 'bg-green-500'
                            }`}
                            style={{ width: `${Math.min(100, q.percentUsed)}%` }}
                          />
                        </div>
                      )}
                    </td>
                    <td className="py-2">
                      <span
                        className={`px-2 py-0.5 rounded-full text-xs ${
                          p.connected ? 'bg-green-50 text-green-600' : 'bg-gray-100 text-gray-500'
                        }`}
                      >
                        {p.connected ? 'Connected' : 'Disconnected'}
                      </span>
                      {h && (
                        <div className="text-xs mt-1 text-gray-500">
                          {h.status}: {h.message}
                        </div>
                      )}
                    </td>
                    <td className="py-2 text-right space-x-3">
                      <button
                        onClick={() => runHealthCheck(p.id)}
                        className="text-brand-600 text-xs hover:underline"
                      >
                        Health
                      </button>
                      {p.connected && (
                        <button
                          onClick={() => disconnect(p.id)}
                          className="text-red-500 text-xs hover:underline"
                        >
                          Disconnect
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
