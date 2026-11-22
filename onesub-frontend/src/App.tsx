import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import DashboardPage from './pages/DashboardPage'
import { SubscriptionsPage, SubscriptionFormPage } from './pages/SubscriptionPages'
import MerchantPage from './pages/MerchantPage'
import PaymentPage from './pages/PaymentPage'
import { CalendarPage, NotificationsPage, ReportsPage, SettingsPage } from './pages/UtilityPages'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/subscriptions" element={<SubscriptionsPage />} />
              <Route path="/subscriptions/new" element={<SubscriptionFormPage />} />
              <Route path="/subscriptions/:id/edit" element={<SubscriptionFormPage />} />
              <Route path="/merchants" element={<MerchantPage />} />
              <Route path="/payments" element={<PaymentPage />} />
              <Route path="/calendar" element={<CalendarPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/profile" element={<SettingsPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
