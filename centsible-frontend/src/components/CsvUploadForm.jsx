import { useState } from 'react'
import api from '../api/client'
import { formatLKR } from '../utils/currency'

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
        positive for income, negative for expenses. Both income and expenses
        can be uploaded together in the same file; new categories are created automatically.
        Bank statement PDFs are also supported.
      </p>

      <div className="upload-controls">
        <input type="file" accept=".csv,.pdf" onChange={handleFileChange} />
        <button onClick={handleUpload} disabled={!file || status === 'uploading'}>
          {status === 'uploading' ? 'Uploading…' : 'Upload'}
        </button>
      </div>

      {status === 'success' && result && (
        <div className="upload-success-card">
          <p className="upload-message upload-message--success">
            Imported <strong>{result.imported}</strong> transaction{result.imported === 1 ? '' : 's'}:{' '}
            {result.incomeCount} income ({formatLKR(result.totalIncome)}) and{' '}
            {result.expenseCount} expense{result.expenseCount === 1 ? '' : 's'} ({formatLKR(result.totalExpense)})
            {result.categoriesCount ? ` across ${result.categoriesCount} categories` : ''}.
            {result.skipped > 0 ? ` Skipped ${result.skipped} row(s).` : ''}
          </p>
          {result.skipped > 0 && result.errors?.length > 0 && (
            <ul className="upload-errors">
              {result.errors.map((err, i) => (
                <li key={i}>{err}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      {status === 'error' && (
        <p className="upload-message upload-message--error">
          Upload failed. Check the backend is running and the file is a valid CSV or PDF.
        </p>
      )}
    </div>
  )
}
