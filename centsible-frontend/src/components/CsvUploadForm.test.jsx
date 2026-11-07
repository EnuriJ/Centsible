import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import CsvUploadForm from './CsvUploadForm'
import api from '../api/client'

vi.mock('../api/client', () => ({
  default: { post: vi.fn() },
  AUTH_TOKEN_KEY: 'centsible_token',
}))

function chooseFile(container) {
  const input = container.querySelector('input[type="file"]')
  const file = new File(['date,description,amount\n'], 'statement.csv', { type: 'text/csv' })
  return { input, file }
}

beforeEach(() => {
  api.post.mockReset()
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

describe('CsvUploadForm', () => {
  it('is switched off in the demo and says why', () => {
    const { container } = render(<CsvUploadForm readOnly />)

    expect(screen.getByText(/Importing is turned off in the demo/)).toBeInTheDocument()
    expect(container.querySelector('input[type="file"]')).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Upload' })).toBeDisabled()
  })

  it('cannot upload even if a file was already chosen when the page became read-only', async () => {
    const user = userEvent.setup()
    const { container, rerender } = render(<CsvUploadForm />)
    const { input, file } = chooseFile(container)
    await user.upload(input, file)
    expect(screen.getByRole('button', { name: 'Upload' })).toBeEnabled()

    rerender(<CsvUploadForm readOnly />)

    expect(screen.getByRole('button', { name: 'Upload' })).toBeDisabled()
  })

  it('keeps Upload disabled until a file is chosen', async () => {
    const user = userEvent.setup()
    const { container } = render(<CsvUploadForm />)
    expect(screen.getByRole('button', { name: 'Upload' })).toBeDisabled()

    const { input, file } = chooseFile(container)
    await user.upload(input, file)

    expect(screen.getByRole('button', { name: 'Upload' })).toBeEnabled()
  })

  it('reports what was imported and tells the page to refresh', async () => {
    api.post.mockResolvedValue({
      data: {
        imported: 5,
        duplicateCount: 1,
        incomeCount: 2,
        expenseCount: 3,
        totalIncome: 250154.46,
        totalExpense: 57350.5,
        parseErrors: [],
      },
    })
    const onUploadSuccess = vi.fn()
    const user = userEvent.setup()
    const { container } = render(<CsvUploadForm onUploadSuccess={onUploadSuccess} />)

    const { input, file } = chooseFile(container)
    await user.upload(input, file)
    await user.click(screen.getByRole('button', { name: 'Upload' }))

    const message = await screen.findByText(/Imported/)
    expect(message).toHaveTextContent('Imported 5 transactions')
    expect(message).toHaveTextContent('Rs. 250,154.46')
    expect(message).toHaveTextContent('1 duplicate skipped')
    expect(onUploadSuccess).toHaveBeenCalledTimes(1)
    expect(api.post).toHaveBeenCalledWith(
      '/transactions/upload',
      expect.any(FormData),
      expect.anything(),
    )
  })

  it('lists rows that could not be read', async () => {
    api.post.mockResolvedValue({
      data: { imported: 0, duplicateCount: 0, parseErrors: ['Row 3: amount is not a number'] },
    })
    const user = userEvent.setup()
    const { container } = render(<CsvUploadForm />)

    const { input, file } = chooseFile(container)
    await user.upload(input, file)
    await user.click(screen.getByRole('button', { name: 'Upload' }))

    expect(await screen.findByText('Row 3: amount is not a number')).toBeInTheDocument()
    expect(screen.getByText(/0 transactions imported/)).toBeInTheDocument()
  })

  it('shows the server’s reason when it refuses the upload (403)', async () => {
    api.post.mockRejectedValue({
      response: { status: 403, data: { error: 'The demo account is read-only.' } },
    })
    const user = userEvent.setup()
    const { container } = render(<CsvUploadForm />)

    const { input, file } = chooseFile(container)
    await user.upload(input, file)
    await user.click(screen.getByRole('button', { name: 'Upload' }))

    await waitFor(() =>
      expect(screen.getByText('The demo account is read-only.')).toBeInTheDocument(),
    )
  })

  it('falls back to a generic message for other failures', async () => {
    api.post.mockRejectedValue(new Error('Network Error'))
    const user = userEvent.setup()
    const { container } = render(<CsvUploadForm />)

    const { input, file } = chooseFile(container)
    await user.upload(input, file)
    await user.click(screen.getByRole('button', { name: 'Upload' }))

    expect(await screen.findByText(/Upload failed\. Check the backend/)).toBeInTheDocument()
  })

  it('refuses a file that is too big before sending anything', async () => {
    const user = userEvent.setup()
    const { container } = render(<CsvUploadForm />)
    const { input, file } = chooseFile(container)
    Object.defineProperty(file, 'size', { value: 11 * 1024 * 1024 })

    await user.upload(input, file)

    expect(screen.getByText(/That file is 11\.0 MB/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Upload' })).toBeDisabled()
    expect(api.post).not.toHaveBeenCalled()
  })

  it('clears the size warning once a smaller file is chosen', async () => {
    const user = userEvent.setup()
    const { container } = render(<CsvUploadForm />)
    const { input, file } = chooseFile(container)
    const big = new File(['x'], 'big.csv', { type: 'text/csv' })
    Object.defineProperty(big, 'size', { value: 11 * 1024 * 1024 })
    await user.upload(input, big)
    expect(screen.getByText(/That file is/)).toBeInTheDocument()

    await user.upload(input, file)

    expect(screen.queryByText(/That file is/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Upload' })).toBeEnabled()
  })

  it('explains a 413 from the server', async () => {
    api.post.mockRejectedValue({ response: { status: 413, data: 'Payload Too Large' } })
    const user = userEvent.setup()
    const { container } = render(<CsvUploadForm />)
    const { input, file } = chooseFile(container)
    await user.upload(input, file)
    await user.click(screen.getByRole('button', { name: 'Upload' }))

    expect(await screen.findByText(/That file is too large/)).toBeInTheDocument()
  })
})
