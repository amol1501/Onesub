export interface User {
  id: number
  fullName: string
  email: string
  timezone: string
  currency: string
  status: string
  createdAt: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface Merchant {
  id: number
  name: string
  category: string
  websiteUrl: string
  logoUrl: string | null
  isActive: boolean
  custom: boolean
}

export interface Subscription {
  id: number
  merchantId: number
  merchantName: string
  planId: number | null
  planName: string | null
  amount: number
  currency: string
  billingCycle: 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  startDate: string
  nextBillingDate: string
  endDate: string | null
  status: 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'EXPIRED'
  reminderEnabled: boolean
}

export interface Payment {
  id: number
  subscriptionId: number
  amount: number
  currency: string
  paymentDate: string
  paymentStatus: 'PAID' | 'PENDING' | 'FAILED'
  notes: string | null
}

export interface DashboardSummary {
  activeSubscriptions: number
  monthlyRecurringExpenditure: Record<string, number>
  annualEstimatedExpenditure: Record<string, number>
  upcomingRenewals: Subscription[]
  recentPayments: Payment[]
  merchantBreakdown: Record<string, number>
}

export interface SpendingReport {
  period: string
  totals: Record<string, number>
  byMerchant: Record<string, number>
}

export interface MerchantWiseReport {
  byCurrencyAndMerchant: Record<string, Record<string, number>>
}

export interface Notification {
  id: number
  subscriptionId: number | null
  notificationType: string
  scheduledAt: string
  sentAt: string | null
  status: string
  read: boolean
}

export interface NotificationPreferences {
  reminder7Days: boolean
  reminder3Days: boolean
  reminder1Day: boolean
  emailEnabled: boolean
}
