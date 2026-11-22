import { apiGet, apiMutation } from './client'
import type {
  DashboardSummary, Merchant, MerchantWiseReport, Notification, NotificationPreferences,
  Page, Payment, SpendingReport, Subscription,
} from './types'

export const merchantApi = {
  list: (page = 0, size = 20) =>
    apiGet<Page<Merchant>>(`/api/v1/merchants?page=${page}&size=${size}&sort=name`),
  get: (id: number) => apiGet<Merchant>(`/api/v1/merchants/${id}`),
  create: (body: Partial<Merchant>) => apiMutation<Merchant>('/api/v1/merchants', 'POST', body),
  update: (id: number, body: Partial<Merchant>) =>
    apiMutation<Merchant>(`/api/v1/merchants/${id}`, 'PUT', body),
  delete: (id: number) => apiMutation<void>(`/api/v1/merchants/${id}`, 'DELETE'),
}

export const subscriptionApi = {
  list: (page = 0, size = 20) =>
    apiGet<Page<Subscription>>(`/api/v1/subscriptions?page=${page}&size=${size}&sort=nextBillingDate`),
  get: (id: number) => apiGet<Subscription>(`/api/v1/subscriptions/${id}`),
  create: (body: Record<string, unknown>) =>
    apiMutation<Subscription>('/api/v1/subscriptions', 'POST', body),
  update: (id: number, body: Record<string, unknown>) =>
    apiMutation<Subscription>(`/api/v1/subscriptions/${id}`, 'PUT', body),
  pause: (id: number) => apiMutation<Subscription>(`/api/v1/subscriptions/${id}/pause`, 'PATCH'),
  resume: (id: number) => apiMutation<Subscription>(`/api/v1/subscriptions/${id}/resume`, 'PATCH'),
  cancel: (id: number) => apiMutation<Subscription>(`/api/v1/subscriptions/${id}/cancel`, 'PATCH'),
}

export const paymentApi = {
  list: (subscriptionId: number, page = 0, size = 20) =>
    apiGet<Page<Payment>>(`/api/v1/subscriptions/${subscriptionId}/payments?page=${page}&size=${size}&sort=paymentDate,desc`),
  create: (subscriptionId: number, body: Record<string, unknown>) =>
    apiMutation<Payment>(`/api/v1/subscriptions/${subscriptionId}/payments`, 'POST', body),
  update: (id: number, body: Record<string, unknown>) =>
    apiMutation<Payment>(`/api/v1/payments/${id}`, 'PUT', body),
  delete: (id: number) => apiMutation<void>(`/api/v1/payments/${id}`, 'DELETE'),
}

export const dashboardApi = {
  summary: () => apiGet<DashboardSummary>('/api/v1/dashboard/summary'),
  upcoming: () => apiGet<Subscription[]>('/api/v1/dashboard/upcoming-renewals'),
}

export const reportApi = {
  monthly: () => apiGet<SpendingReport>('/api/v1/reports/monthly'),
  yearly: () => apiGet<SpendingReport>('/api/v1/reports/yearly'),
  merchantWise: () => apiGet<MerchantWiseReport>('/api/v1/reports/merchant-wise'),
}

export const notificationApi = {
  list: (page = 0, size = 20) =>
    apiGet<Page<Notification>>(`/api/v1/notifications?page=${page}&size=${size}&sort=scheduledAt,desc`),
  markRead: (id: number) =>
    apiMutation<Notification>(`/api/v1/notifications/${id}/read`, 'PATCH'),
  preferences: () =>
    apiGet<NotificationPreferences>('/api/v1/notifications/preferences'),
  updatePreferences: (body: NotificationPreferences) =>
    apiMutation<NotificationPreferences>('/api/v1/notifications/preferences', 'PUT', body),
}
