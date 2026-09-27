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

      {status === 'success' && result && (() => {
        const imported = result.imported ?? 0
        const duplicateCount = result.duplicateCount ?? 0
        const parseErrors = result.parseErrors ?? result.errors ?? []
        const errorCount = parseErrors.length

        const messageClass =
          errorCount > 0
            ? imported > 0 || duplicateCount > 0
              ? 'upload-message--warning'
              : 'upload-message--error'
            : 'upload-message--success'

        return (
          <div className="upload-success-card">
            <p className={`upload-message ${messageClass}`}>
              {imported > 0 ? (
                <>
                  Imported <strong>{imported}</strong> transaction{imported === 1 ? '' : 's'}:{' '}
                  {result.incomeCount} income ({formatLKR(result.totalIncome)}) and{' '}
                  {result.expenseCount} expense{result.expenseCount === 1 ? '' : 's'} ({formatLKR(result.totalExpense)})
                  {result.categoriesCount ? ` across ${result.categoriesCount} categories` : ''}.
                  {duplicateCount > 0 ? ` ${duplicateCount} duplicate${duplicateCount === 1 ? '' : 's'} skipped.` : ''}
                  {errorCount > 0 ? ` ${errorCount} row${errorCount === 1 ? '' : 's'} had errors — see below.` : ''}
                </>
              ) : duplicateCount > 0 && errorCount === 0 ? (
                `${duplicateCount} row${duplicateCount === 1 ? '' : 's'} skipped as duplicate${duplicateCount === 1 ? '' : 's'} (already imported).`
              ) : duplicateCount > 0 ? (
                `0 new transactions imported. ${duplicateCount} row${duplicateCount === 1 ? '' : 's'} skipped as duplicate${duplicateCount === 1 ? '' : 's'}. ${errorCount} row${errorCount === 1 ? '' : 's'} had errors — see below.`
              ) : errorCount > 0 ? (
                `0 transactions imported. ${errorCount} row${errorCount === 1 ? '' : 's'} had errors — see below.`
              ) : (
                'No transactions found to import.'
              )}
            </p>
            {errorCount > 0 && (
              <div className="upload-errors-container">
                <ul className="upload-errors">
                  {parseErrors.map((err, i) => (
                    <li key={i}>{err}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )
      })()}

      {status === 'error' && (
        <p className="upload-message upload-message--error">
          Upload failed. Check the backend is running and the file is a valid CSV or PDF.
        </p>
      )}
    </div>
  )
}
