import { useEffect, useState } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts'
import { notificationApi, reportApi, subscriptionApi } from '../api/modules'
import { useAuth } from '../context/AuthContext'
import { authApi } from '../api/authApi'
import type { Notification, NotificationPreferences, SpendingReport, Subscription } from '../api/types'

export function CalendarPage() {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([])
  const [error, setError] = useState('')
  useEffect(() => { subscriptionApi.list(0, 100).then((page) => setSubscriptions(page.content)).catch((ex) => setError(ex.message)) }, [])
  const sorted = [...subscriptions].sort((a, b) => a.nextBillingDate.localeCompare(b.nextBillingDate))
  return <><div className="page-header"><div><h1>Renewal Calendar</h1><p>Upcoming billing dates</p></div></div>{error && <div className="error">{error}</div>}<div className="panel calendar-list">{sorted.map((item) => <div className="calendar-row" key={item.id}><time>{item.nextBillingDate}</time><strong>{item.merchantName}</strong><span>{item.amount} {item.currency}</span></div>)}</div></>
}

export function ReportsPage() {
  const [monthly, setMonthly] = useState<SpendingReport | null>(null)
  const [yearly, setYearly] = useState<SpendingReport | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    Promise.all([reportApi.monthly(), reportApi.yearly()])
      .then(([month, year]) => { setMonthly(month); setYearly(year) })
      .catch((ex) => setError(ex.message))
  }, [])
  const chartData = monthly ? Object.entries(monthly.byMerchant).map(([name, value]) => ({ name, value })) : []
  return <><div className="page-header"><div><h1>Reports and Analytics</h1><p>Monthly and annual spending estimates</p></div></div>{error && <div className="error">{error}</div>}
    <div className="card-grid">{monthly && Object.entries(monthly.totals).map(([currency, value]) => <div className="metric-card" key={`m-${currency}`}><span>Monthly · {currency}</span><strong>{value.toFixed(2)}</strong></div>)}{yearly && Object.entries(yearly.totals).map(([currency, value]) => <div className="metric-card" key={`y-${currency}`}><span>Yearly · {currency}</span><strong>{value.toFixed(2)}</strong></div>)}</div>
    <section className="panel"><h2>Monthly spending by merchant</h2><div className="chart"><ResponsiveContainer><BarChart data={chartData}><XAxis dataKey="name" /><YAxis /><Tooltip /><Bar dataKey="value" fill="#2563eb" radius={[6, 6, 0, 0]} /></BarChart></ResponsiveContainer></div></section>
  </>
}

export function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [preferences, setPreferences] = useState<NotificationPreferences | null>(null)
  const [error, setError] = useState('')
  const load = () => Promise.all([notificationApi.list(), notificationApi.preferences()])
    .then(([page, prefs]) => { setNotifications(page.content); setPreferences(prefs) })
    .catch((ex) => setError(ex.message))
  useEffect(() => { void load() }, [])
  const markRead = async (id: number) => { await notificationApi.markRead(id); await load() }
  return <><div className="page-header"><div><h1>Notifications</h1><p>Renewal reminder history</p></div></div>{error && <div className="error">{error}</div>}<div className="panel notification-list">{notifications.map((item) => <div className={`notification-row ${item.read ? '' : 'unread'}`} key={item.id}><div><strong>{item.notificationType.replaceAll('_', ' ')}</strong><small>{new Date(item.scheduledAt).toLocaleString()}</small></div>{!item.read && <button className="button ghost" onClick={() => void markRead(item.id)}>Mark read</button>}</div>)}{!notifications.length && <div className="empty">No notifications</div>}</div>{preferences && <div className="panel notification-summary"><strong>Reminder preferences</strong><span>7 days: {preferences.reminder7Days ? 'On' : 'Off'} · 3 days: {preferences.reminder3Days ? 'On' : 'Off'} · 1 day: {preferences.reminder1Day ? 'On' : 'Off'}</span></div>}</>
}

export function SettingsPage() {
  const { user, refresh } = useAuth()
  const [fullName, setFullName] = useState(user?.fullName || '')
  const [timezone, setTimezone] = useState(user?.timezone || 'UTC')
  const [currency, setCurrency] = useState(user?.currency || 'USD')
  const [prefs, setPrefs] = useState<NotificationPreferences | null>(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => { if (user) { setFullName(user.fullName); setTimezone(user.timezone); setCurrency(user.currency) }; notificationApi.preferences().then(setPrefs).catch((ex) => setError(ex.message)) }, [user])

  const saveProfile = async () => {
    setError('')
    try { await authApi.updateMe({ fullName, timezone, currency }); await refresh(); setMessage('Profile updated') } catch (ex) { setError(ex instanceof Error ? ex.message : 'Unable to update profile') }
  }
  const savePreferences = async () => {
    if (!prefs) return
    try { await notificationApi.updatePreferences(prefs); setMessage('Notification preferences saved') } catch (ex) { setError(ex instanceof Error ? ex.message : 'Unable to save preferences') }
  }

  return <>
    <div className="page-header"><div><h1>Settings</h1><p>User profile and notification preferences</p></div></div>
    {error && <div className="error">{error}</div>}
    {message && <div className="success">{message}</div>}
    <section className="panel form-grid">
      <h2 className="span-two">Profile</h2>
      <label>Full name<input value={fullName} onChange={(event) => setFullName(event.target.value)} /></label>
      <label>Timezone<input value={timezone} onChange={(event) => setTimezone(event.target.value)} /></label>
      <label>Currency<input maxLength={3} value={currency} onChange={(event) => setCurrency(event.target.value.toUpperCase())} /></label>
      <div className="span-two form-actions"><button className="button primary" onClick={() => void saveProfile()}>Save profile</button></div>
    </section>
    {prefs && <section className="panel">
      <h2>Renewal reminders</h2>
      <div className="preference-list">
        <label className="check-row"><input type="checkbox" checked={prefs.reminder7Days} onChange={(event) => setPrefs({ ...prefs, reminder7Days: event.target.checked })} /> 7 days before</label>
        <label className="check-row"><input type="checkbox" checked={prefs.reminder3Days} onChange={(event) => setPrefs({ ...prefs, reminder3Days: event.target.checked })} /> 3 days before</label>
        <label className="check-row"><input type="checkbox" checked={prefs.reminder1Day} onChange={(event) => setPrefs({ ...prefs, reminder1Day: event.target.checked })} /> 1 day before</label>
        <label className="check-row"><input type="checkbox" checked={prefs.emailEnabled} onChange={(event) => setPrefs({ ...prefs, emailEnabled: event.target.checked })} /> Email notifications</label>
      </div>
      <button className="button primary" onClick={() => void savePreferences()}>Save preferences</button>
    </section>}
  </>
}
