import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { expect, test, vi } from 'vitest'
import { LoginPage } from '../pages/AuthPages'

const login = vi.fn()
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ login }),
}))

test('renders login form and submits credentials', async () => {
  login.mockResolvedValue(undefined)
  render(<MemoryRouter><LoginPage /></MemoryRouter>)
  await userEvent.type(screen.getByLabelText('Email'), 'user@example.com')
  await userEvent.type(screen.getByLabelText('Password'), 'password123')
  await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
  expect(login).toHaveBeenCalledWith('user@example.com', 'password123')
})
