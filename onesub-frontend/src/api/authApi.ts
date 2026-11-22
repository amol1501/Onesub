import { apiMutation, apiGet } from './client'
import type { User } from './types'

export const authApi = {
  register: (body: { fullName: string; email: string; password: string; timezone?: string; currency?: string }) =>
    apiMutation<User>('/api/v1/auth/register', 'POST', body),
  login: (body: { email: string; password: string }) =>
    apiMutation<User>('/api/v1/auth/login', 'POST', body),
  logout: () => apiMutation<void>('/api/v1/auth/logout', 'POST'),
  me: () => apiGet<User>('/api/v1/users/me'),
  updateMe: (body: { fullName: string; timezone: string; currency: string }) =>
    apiMutation<User>('/api/v1/users/me', 'PUT', body),
}
