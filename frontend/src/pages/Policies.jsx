import { useEffect, useState } from 'react'
import api from '../api/axios'

const INDUSTRIES = ['HEALTHCARE', 'FINANCE', 'EDUCATION', 'MANUFACTURING', 'GENERAL']

export default function Policies() {
  const [policies, setPolicies] = useState([])
  const [presets, setPresets] = useState([])
  const [name, setName] = useState('')
  const [industry, setIndustry] = useState('GENERAL')
  const [customCron, setCustomCron] = useState('')
  const [retentionDays, setRetentionDays] = useState('')
  const [encryptionEnabled, setEncryptionEnabled] = useState(true)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = () => api.get('/policies').then((res) => setPolicies(res.data))

  useEffect(() => {
    load()
    api.get('/policies/industry-presets').then((res) => setPresets(res.data))
  }, [])

  const create = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await api.post('/policies', {
        name,
        industry,
        cronExpression: customCron || undefined,
        retentionDays: retentionDays ? Number(retentionDays) : undefined,
        encryptionEnabled,
      })
      setName('')
      setCustomCron('')
      setRetentionDays('')
      load()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create policy')
    } finally {
      setLoading(false)
    }
  }

  const deactivate = async (id) => {
    await api.delete(`/policies/${id}`)
    load()
  }

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">Backup Policies</h2>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Create Policy</h3>
        {error && <div className="mb-3 text-sm bg-red-50 text-red-600 border border-red-100 rounded-md px-3 py-2">{error}</div>}
        <form onSubmit={create} className="grid grid-cols-1 md:grid-cols-5 gap-3 items-end">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Policy Name</label>
            <input className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Industry Preset</label>
            <select className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" value={industry} onChange={(e) => setIndustry(e.target.value)}>
              {INDUSTRIES.map((i) => <option key={i} value={i}>{i}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Cron Override (optional)</label>
            <input className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" placeholder="uses industry default" value={customCron} onChange={(e) => setCustomCron(e.target.value)} />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Retention Days (optional)</label>
            <input type="number" className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" placeholder="uses industry default" value={retentionDays} onChange={(e) => setRetentionDays(e.target.value)} />
          </div>
          <div className="flex items-center gap-2">
            <input type="checkbox" checked={encryptionEnabled} onChange={(e) => setEncryptionEnabled(e.target.checked)} />
            <label className="text-sm text-gray-600">AES-256 Encryption</label>
          </div>
          <button disabled={loading} className="md:col-span-5 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium py-2 rounded-md transition disabled:opacity-60">
            {loading ? 'Creating...' : 'Create Policy'}
          </button>
        </form>

        {presets.length > 0 && (
          <div className="mt-4 text-xs text-gray-400 grid grid-cols-1 md:grid-cols-2 gap-1">
            {presets.map((p) => (
              <p key={p}>{p}: default cron applies, industry-specific retention</p>
            ))}
          </div>
        )}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Your Policies</h3>
        {policies.length === 0 ? (
          <p className="text-sm text-gray-400">No policies yet.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500 border-b border-gray-100">
                <th className="pb-2">Name</th>
                <th className="pb-2">Industry</th>
                <th className="pb-2">Cron</th>
                <th className="pb-2">Retention</th>
                <th className="pb-2">Encrypted</th>
                <th className="pb-2"></th>
              </tr>
            </thead>
            <tbody>
              {policies.map((p) => (
                <tr key={p.id} className="border-b border-gray-50">
                  <td className="py-2">{p.name}</td>
                  <td className="py-2">{p.industry}</td>
                  <td className="py-2 font-mono text-xs">{p.cronExpression}</td>
                  <td className="py-2">{p.retentionDays}d</td>
                  <td className="py-2">{p.encryptionEnabled ? 'Yes' : 'No'}</td>
                  <td className="py-2 text-right">
                    {p.active && <button onClick={() => deactivate(p.id)} className="text-red-500 text-xs hover:underline">Deactivate</button>}
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
