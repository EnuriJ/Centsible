import { beforeEach, describe, expect, it, vi } from 'vitest'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AuthForm from './AuthForm'
import { DEMO_EMAIL, DEMO_PASSWORD } from '../utils/demo'
import api from '../api/client'

const login = vi.fn()
const signup = vi.fn()

vi.mock('../context/useAuth', () => ({
  useAuth: () => ({ login, signup }),
}))

vi.mock('../api/client', () => ({
  default: { get: vi.fn(() => Promise.resolve({ data: { status: 'ok' } })) },
  AUTH_TOKEN_KEY: 'centsible_token',
}))

beforeEach(() => {
  login.mockReset()
  signup.mockReset()
  api.get.mockClear()
  vi.spyOn(console, 'error').mockImplementation(() => {})
})

describe('AuthForm', () => {
  it('signs in with the email and password that were typed', async () => {
    login.mockResolvedValue({})
    const user = userEvent.setup()
    render(<AuthForm />)

    await user.type(screen.getByLabelText('Email Address'), 'me@example.com')
    await user.type(screen.getByLabelText('Password'), 'correct horse battery')
    await user.click(screen.getByRole('button', { name: 'Sign In to Centsible' }))

    expect(login).toHaveBeenCalledWith('me@example.com', 'correct horse battery')
  })

  it('shows the server message when sign-in fails', async () => {
    login.mockRejectedValue({ response: { data: { error: 'Invalid email or password' } } })
    const user = userEvent.setup()
    render(<AuthForm />)

    await user.type(screen.getByLabelText('Email Address'), 'me@example.com')
    await user.type(screen.getByLabelText('Password'), 'wrong password')
    await user.click(screen.getByRole('button', { name: 'Sign In to Centsible' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
  })

  it('asks for a display name only when creating an account', async () => {
    const user = userEvent.setup()
    render(<AuthForm />)
    expect(screen.queryByLabelText('Full / Display Name')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Create Account' }))

    expect(screen.getByLabelText('Full / Display Name')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toHaveAttribute('minlength', '10')
  })

  it('creates an account with the details entered', async () => {
    signup.mockResolvedValue({})
    const user = userEvent.setup()
    render(<AuthForm />)

    await user.click(screen.getByRole('button', { name: 'Create Account' }))
    await user.type(screen.getByLabelText('Full / Display Name'), 'Test Person')
    await user.type(screen.getByLabelText('Email Address'), 'new@example.com')
    await user.type(screen.getByLabelText('Password'), 'a-long-enough-password')
    await user.click(screen.getAllByRole('button', { name: 'Create Account' })[1])

    expect(signup).toHaveBeenCalledWith('new@example.com', 'a-long-enough-password', 'Test Person')
  })

  it('"Try the demo" signs in as the demo account without typing anything', async () => {
    login.mockResolvedValue({})
    const user = userEvent.setup()
    render(<AuthForm />)

    await user.click(screen.getByRole('button', { name: 'Try the demo' }))

    expect(login).toHaveBeenCalledWith(DEMO_EMAIL, DEMO_PASSWORD)
  })

  it('explains when the demo cannot be reached', async () => {
    login.mockRejectedValue(new Error('Network Error'))
    const user = userEvent.setup()
    render(<AuthForm />)

    await user.click(screen.getByRole('button', { name: 'Try the demo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/backend server is running/i)
  })

  it('has a privacy note that says uploaded files are not kept', () => {
    render(<AuthForm />)
    expect(screen.getByText(/Privacy & your data/)).toBeInTheDocument()
    expect(screen.getByText(/is not kept/)).toBeInTheDocument()
  })

  it('pings the server on load so a sleeping one starts waking up', () => {
    render(<AuthForm />)
    expect(api.get).toHaveBeenCalledWith('/health')
  })

  it('does not break if the wake-up ping fails', () => {
    api.get.mockRejectedValueOnce(new Error('Network Error'))
    render(<AuthForm />)
    expect(screen.getByRole('button', { name: 'Try the demo' })).toBeEnabled()
  })

  describe('when the server is slow to answer', () => {
    it('explains the wait after a few seconds, and stops once it answers', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      try {
        let answer
        login.mockReturnValue(new Promise((resolve) => (answer = resolve)))
        const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
        render(<AuthForm />)

        await user.click(screen.getByRole('button', { name: 'Try the demo' }))
        expect(screen.queryByText(/Waking the server up/)).not.toBeInTheDocument()

        await act(async () => {
          vi.advanceTimersByTime(4500)
        })
        expect(screen.getByRole('status')).toHaveTextContent(/Waking the server up/)

        await act(async () => {
          answer({})
        })
        expect(screen.queryByText(/Waking the server up/)).not.toBeInTheDocument()
      } finally {
        vi.useRealTimers()
      }
    })
  })
})
