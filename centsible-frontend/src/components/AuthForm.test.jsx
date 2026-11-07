import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import AuthForm from './AuthForm'
import { DEMO_EMAIL, DEMO_PASSWORD } from '../utils/demo'

const login = vi.fn()
const signup = vi.fn()

vi.mock('../context/useAuth', () => ({
  useAuth: () => ({ login, signup }),
}))

beforeEach(() => {
  login.mockReset()
  signup.mockReset()
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
})
