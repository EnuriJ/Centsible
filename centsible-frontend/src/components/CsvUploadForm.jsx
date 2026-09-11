import { useState } from 'react'
import api from '../api/client'

export default function CsvUploadForm({ onUploadSuccess }) {
  const [file, setFile] = useState(null)
  const [status, setStatus] = useState('idle') // idle | uploading | success | error
  const [result, setResult] = useState(null)

  function handleFileChange(e) {
    setFile(e.target.files[0] ?? null)
    setStatus('idle')
    setResult(null)
  }

  async function handleUpload() {
    if (!file) return
    setStatus('uploading')

    const formData = new FormData()
    formData.append('file', file)

    try {
      const res = await api.post('/transactions/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setResult(res.data)
      setStatus('success')
      onUploadSuccess?.()
    } catch (err) {
      console.error('Upload failed:', err)
      setStatus('error')
    }
  }

  return (
    <div className="upload-card">
      <h2>Import Transactions</h2>
      <p className="upload-hint">
        CSV columns: <code>date, description, amount, category</code> — amount
        negative for expenses, positive for income. New categories are
        created automatically.
      </p>

      <div className="upload-controls">
        <input type="file" accept=".csv" onChange={handleFileChange} />
        <button onClick={handleUpload} disabled={!file || status === 'uploading'}>
          {status === 'uploading' ? 'Uploading…' : 'Upload'}
        </button>
      </div>

      {status === 'success' && result && (
        <p className="upload-message upload-message--success">
          Imported {result.imported} transaction{result.imported === 1 ? '' : 's'}
          {result.skipped > 0 ? `, skipped ${result.skipped} row(s) with errors.` : '.'}
        </p>
      )}
      {status === 'success' && result?.skipped > 0 && (
        <ul className="upload-errors">
          {result.errors.map((err, i) => (
            <li key={i}>{err}</li>
          ))}
        </ul>
      )}
      {status === 'error' && (
        <p className="upload-message upload-message--error">
          Upload failed. Check the backend is running and the file is a valid CSV.
        </p>
      )}
    </div>
  )
}
