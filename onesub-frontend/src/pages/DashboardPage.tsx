import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { dashboardApi } from '../api/modules'
import type { DashboardSummary } from '../api/types'
import { PieChart, Pie, Tooltip, ResponsiveContainer, Cell } from 'recharts'

export default function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    dashboardApi.summary().then(setSummary).catch((ex) => setError(ex.message))
  }, [])

  if (error) return <div className="error">{error}</div>
  if (!summary) return <div className="loading">Loading dashboard...</div>

  const monthly = Object.entries(summary.monthlyRecurringExpenditure)
  const chartData = Object.entries(summary.merchantBreakdown).map(([name, value]) => ({ name, value }))
  const colors = ['#2563eb', '#0ea5e9', '#10b981', '#f59e0b', '#8b5cf6']

  return <>
    <div className="page-header"><div><h1>Dashboard</h1><p>Overview of your subscription spending</p></div><Link className="button primary" to="/subscriptions/new">Add subscription</Link></div>
    <div className="card-grid">
      <div className="metric-card"><span>Active subscriptions</span><strong>{summary.activeSubscriptions}</strong></div>
      {monthly.map(([currency, amount]) => <div className="metric-card" key={currency}><span>Monthly spending · {currency}</span><strong>{amount.toFixed(2)}</strong></div>)}
      {Object.entries(summary.annualEstimatedExpenditure).map(([currency, amount]) => <div className="metric-card" key={currency}><span>Annual spending · {currency}</span><strong>{amount.toFixed(2)}</strong></div>)}
    </div>
    <div className="dashboard-grid">
      <section className="panel"><h2>Subscription breakdown</h2>
        {chartData.length ? <div className="chart"><ResponsiveContainer><PieChart><Pie data={chartData} dataKey="value" nameKey="name" outerRadius={90}>{chartData.map((_, i) => <Cell key={i} fill={colors[i % colors.length]} />)}</Pie><Tooltip /></PieChart></ResponsiveContainer></div> : <div className="empty">No active subscriptions</div>}
      </section>
      <section className="panel"><h2>Upcoming renewals</h2>
        {summary.upcomingRenewals.length ? summary.upcomingRenewals.map((item) => <div className="list-row" key={item.id}><div><strong>{item.merchantName}</strong><small>{item.nextBillingDate}</small></div><b>{item.amount} {item.currency}</b></div>) : <div className="empty">No renewals in the next 30 days</div>}
      </section>
    </div>
  </>
}
