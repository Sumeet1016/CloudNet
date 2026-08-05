import { useEffect, useState } from 'react'
import api from '../api/axios'

const STATUS_STYLES = {
  SUCCESS: 'bg-green-50 text-green-600',
  FAILED: 'bg-red-50 text-red-600',
  IN_PROGRESS: 'bg-yellow-50 text-yellow-600',
  PENDING: 'bg-gray-100 text-gray-500',
  RESTORED: 'bg-blue-50 text-blue-600',
}

export default function BackupJobs() {
  const [jobs, setJobs] = useState([])
  const [providers, setProviders] = useState([])
  const [policies, setPolicies] = useState([])
  const [providerId, setProviderId] = useState('')
  const [policyId, setPolicyId] = useState('')
  const [jobName, setJobName] = useState('')
  const [files, setFiles] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [expandedJob, setExpandedJob] = useState(null)
  const [jobFiles, setJobFiles] = useState({})

  const loadJobs = () => api.get('/backups').then((res) => setJobs(res.data))

  useEffect(() => {
    loadJobs()
    api.get('/cloud-providers').then((res) => setProviders(res.data.filter((p) => p.connected)))
    api.get('/policies').then((res) => setPolicies(res.data.filter((p) => p.active)))
  }, [])

  const runBackup = async (e) => {
    e.preventDefault()
    if (!providerId || !files || files.length === 0) {
      setError('Select a provider and at least one file')
      return
    }
    setError('')
    setLoading(true)
    try {
      const formData = new FormData()
      formData.append('cloudProviderId', providerId)
      if (policyId) formData.append('policyId', policyId)
      if (jobName) formData.append('jobName', jobName)
      Array.from(files).forEach((f) => formData.append('files', f))

      await api.post('/backups/run', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setJobName('')
      setFiles(null)
      loadJobs()
    } catch (err) {
      setError(err.response?.data?.message || 'Backup failed')
    } finally {
      setLoading(false)
    }
  }

  const toggleExpand = async (jobId) => {
    if (expandedJob === jobId) {
      setExpandedJob(null)
      return
    }
    setExpandedJob(jobId)
    if (!jobFiles[jobId]) {
      const res = await api.get(`/backups/${jobId}/files`)
      setJobFiles((prev) => ({ ...prev, [jobId]: res.data }))
    }
  }

  const restore = async (backupFileId, fileName) => {
    const res = await api.get(`/backups/restore/${backupFileId}`, { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    link.remove()
  }

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-800">Backup Jobs</h2>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Run a Manual Backup</h3>
        {error && <div className="mb-3 text-sm bg-red-50 text-red-600 border border-red-100 rounded-md px-3 py-2">{error}</div>}
        <form onSubmit={runBackup} className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Cloud Provider</label>
            <select className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" value={providerId} onChange={(e) => setProviderId(e.target.value)} required>
              <option value="">Select...</option>
              {providers.map((p) => <option key={p.id} value={p.id}>{p.displayName}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Policy (optional)</label>
            <select className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" value={policyId} onChange={(e) => setPolicyId(e.target.value)}>
              <option value="">None</option>
              {policies.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Job Name (optional)</label>
            <input className="w-full border border-gray-200 rounded-md px-3 py-2 text-sm" value={jobName} onChange={(e) => setJobName(e.target.value)} />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Files</label>
            <input type="file" multiple className="w-full text-sm" onChange={(e) => setFiles(e.target.files)} />
          </div>
          <button disabled={loading} className="md:col-span-4 bg-brand-600 hover:bg-brand-700 text-white text-sm font-medium py-2 rounded-md transition disabled:opacity-60">
            {loading ? 'Backing up...' : 'Run Backup'}
          </button>
        </form>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
        <h3 className="font-semibold text-gray-700 mb-4">Job History</h3>
        {jobs.length === 0 ? (
          <p className="text-sm text-gray-400">No backup jobs yet.</p>
        ) : (
          <div className="space-y-2">
            {jobs.map((job) => (
              <div key={job.id} className="border border-gray-100 rounded-lg">
                <button onClick={() => toggleExpand(job.id)} className="w-full flex items-center justify-between px-4 py-3 text-left text-sm">
                  <div>
                    <p className="font-medium text-gray-700">{job.jobName}</p>
                    <p className="text-xs text-gray-400">{job.cloudProvider?.type} · {job.scheduled ? 'Scheduled' : 'Manual'}</p>
                  </div>
                  <span className={`px-2 py-0.5 rounded-full text-xs ${STATUS_STYLES[job.status] || ''}`}>{job.status}</span>
                </button>
                {expandedJob === job.id && (
                  <div className="px-4 pb-3 border-t border-gray-50">
                    {job.errorMessage && <p className="text-xs text-red-500 mt-2">{job.errorMessage}</p>}
                    {(jobFiles[job.id] || []).map((f) => (
                      <div key={f.id} className="flex items-center justify-between text-xs text-gray-600 py-1.5">
                        <span>{f.originalFileName} (v{f.versionNumber}) - {(f.fileSizeBytes / 1024).toFixed(1)} KB</span>
                        <button onClick={() => restore(f.id, f.originalFileName)} className="text-brand-600 hover:underline">Restore</button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
