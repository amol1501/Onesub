import { describe, expect, it, vi, afterEach } from 'vitest'
import { apiGet } from '../api/client'

afterEach(() => vi.restoreAllMocks())

describe('API client', () => {
  it('returns API response data', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ success: true, message: 'Success', data: { id: 1 }, timestamp: '' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    )))
    await expect(apiGet<{ id: number }>('/test')).resolves.toEqual({ id: 1 })
  })

  it('surfaces backend API errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ success: false, message: 'Forbidden', errorCode: 'FORBIDDEN', validationErrors: {}, timestamp: '' }),
      { status: 403, headers: { 'Content-Type': 'application/json' } },
    )))
    await expect(apiGet('/test')).rejects.toThrow('Forbidden')
  })
})
