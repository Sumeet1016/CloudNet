import { useEffect, useState } from 'react'
import api from '../api/axios'

export default function Schedules() {
  const [schedules, setSchedules] = useState([])
  const [providers, setProviders] = useState([])
  const [policies, setPolicies] = useState([])
  const [providerId, setProviderId] = useState('')
  const [policyId, setPolicyId] = useState('')
  const [sourcePath, setSourcePath] = useState('')
  const [cronOverride, setCronOverride] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = () => api.get('/schedules').then((res) => setSchedules(res.data))

  useEffect(() => {
    load()
    api.get('/cloud-providers').then((res) => setProviders(res.data.filter((p) => p.connected)))
    api.get('/policies').then((res) => setPolicies(res.data.filter((p) => p.active)))
  }, [])

  const create = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await api.post('/schedules', {
        cloudProviderId: providerId,
        policyId,
        sourcePath,
        cronExpression: cronOverride || undefined,
      })
      setSourcePath('')
      setCronOverride('')
      load()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create schedule')
    } finally {
      setLoading(false)
    }
  }

  const deactivate = async (id) => {
    await api.delete(`/schedules/${id}`)
    load()
  }

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">Backup Schedules</h2>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Create Schedule</h3>
        <p className="text-xs text-gray-400 mb-3">
          The scheduler checks every minute for due jobs and automatically backs up every file
          in the source folder using the linked policy's cron expression and retention rules.
        </p>
        {error && <div className="mb-3 text-sm bg-red-50 text-red-600 border border-red-100 rounded-md px-3 py-2">{error}</div>}
        <form onSubmit={create} className="grid grid-cols-1 md:grid-cols-5 gap-3 items-end">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Cloud Provider</label>
            <select className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" value={providerId} onChange={(e) => setProviderId(e.target.value)} required>
              <option value="">Select...</option>
              {providers.map((p) => <option key={p.id} value={p.id}>{p.displayName}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Policy</label>
            <select className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" value={policyId} onChange={(e) => setPolicyId(e.target.value)} required>
              <option value="">Select...</option>
              {policies.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Source Folder Path</label>
            <input className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" placeholder="/home/user/documents" value={sourcePath} onChange={(e) => setSourcePath(e.target.value)} required />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Cron Override (optional)</label>
            <input className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" placeholder="uses policy default" value={cronOverride} onChange={(e) => setCronOverride(e.target.value)} />
          </div>
          <button disabled={loading} className="bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium py-2 rounded-md transition disabled:opacity-60">
            {loading ? 'Saving...' : 'Create Schedule'}
          </button>
        </form>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Active Schedules</h3>
        {schedules.length === 0 ? (
          <p className="text-sm text-gray-400">No schedules yet.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500 border-b border-gray-100">
                <th className="pb-2">Source Path</th>
                <th className="pb-2">Cron</th>
                <th className="pb-2">Next Run</th>
                <th className="pb-2">Last Run</th>
                <th className="pb-2"></th>
              </tr>
            </thead>
            <tbody>
              {schedules.map((s) => (
                <tr key={s.id} className="border-b border-gray-50">
                  <td className="py-2 font-mono text-xs">{s.sourcePath}</td>
                  <td className="py-2 font-mono text-xs">{s.cronExpression}</td>
                  <td className="py-2 text-xs">{s.nextRunAt ? new Date(s.nextRunAt).toLocaleString() : '-'}</td>
                  <td className="py-2 text-xs">{s.lastRunAt ? new Date(s.lastRunAt).toLocaleString() : 'Never'}</td>
                  <td className="py-2 text-right">
                    {s.active && <button onClick={() => deactivate(s.id)} className="text-red-500 text-xs hover:underline">Deactivate</button>}
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
