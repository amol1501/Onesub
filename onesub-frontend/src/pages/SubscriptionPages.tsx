import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { merchantApi, subscriptionApi } from '../api/modules'
import type { Merchant } from '../api/types'

export function SubscriptionsPage() {
  const [page, setPage] = useState<Awaited<ReturnType<typeof subscriptionApi.list>> | null>(null)
  const [error, setError] = useState('')
  const load = () => subscriptionApi.list().then(setPage).catch((ex) => setError(ex.message))
  useEffect(() => { void load() }, [])

  const mutate = async (id: number, action: 'pause' | 'resume' | 'cancel') => {
    await subscriptionApi[action](id)
    await load()
  }

  if (error) return <div className="error">{error}</div>
  return <>
    <div className="page-header"><div><h1>My Subscriptions</h1><p>Track and manage recurring subscriptions</p></div><Link className="button primary" to="/subscriptions/new">Add subscription</Link></div>
    <div className="panel table-wrap">
      <table><thead><tr><th>Merchant</th><th>Plan</th><th>Amount</th><th>Cycle</th><th>Next renewal</th><th>Status</th><th>Actions</th></tr></thead>
      <tbody>{page?.content.map((item) => <tr key={item.id}>
        <td><strong>{item.merchantName}</strong></td><td>{item.planName || 'Custom'}</td><td>{item.amount} {item.currency}</td><td>{item.billingCycle}</td><td>{item.nextBillingDate}</td><td><span className={`status ${item.status.toLowerCase()}`}>{item.status}</span></td>
        <td className="actions"><Link to={`/subscriptions/${item.id}/edit`}>Edit</Link>{item.status === 'ACTIVE' ? <button onClick={() => void mutate(item.id, 'pause')}>Pause</button> : item.status === 'PAUSED' ? <button onClick={() => void mutate(item.id, 'resume')}>Resume</button> : null}{item.status !== 'CANCELLED' && <button className="danger-text" onClick={() => confirm('Cancel tracking for this subscription?') && void mutate(item.id, 'cancel')}>Cancel</button>}</td>
      </tr>)}</tbody></table>
      {!page?.content.length && <div className="empty">No subscriptions yet</div>}
    </div>
  </>
}

export function SubscriptionFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [merchants, setMerchants] = useState<Merchant[]>([])
  const [error, setError] = useState('')
  const { register, handleSubmit, reset, formState: { isSubmitting } } = useForm<Record<string, unknown>>()

  useEffect(() => {
    merchantApi.list(0, 100).then((page) => setMerchants(page.content)).catch((ex) => setError(ex.message))
    if (id) subscriptionApi.get(Number(id)).then((item) => reset({
      merchantId: item.merchantId, customPlanName: item.planName || '', amount: item.amount,
      currency: item.currency, billingCycle: item.billingCycle, startDate: item.startDate,
      nextBillingDate: item.nextBillingDate, reminderEnabled: item.reminderEnabled,
    })).catch((ex) => setError(ex.message))
  }, [id, reset])

  const submit = handleSubmit(async (values) => {
    setError('')
    const body = { ...values, merchantId: Number(values.merchantId), amount: Number(values.amount), planId: null }
    try {
      if (id) await subscriptionApi.update(Number(id), body)
      else await subscriptionApi.create(body)
      navigate('/subscriptions')
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'Unable to save subscription')
    }
  })

  return <>
    <div className="page-header"><div><h1>{id ? 'Edit subscription' : 'Add subscription'}</h1><p>Manual tracking only. No merchant access or automatic payments.</p></div></div>
    <form className="panel form-grid" onSubmit={submit}>
      {error && <div className="error span-two">{error}</div>}
      <label>Merchant<select required {...register('merchantId')}>{merchants.map((merchant) => <option key={merchant.id} value={merchant.id}>{merchant.name}</option>)}</select></label>
      <label>Plan name<input {...register('customPlanName')} placeholder="e.g. Premium" /></label>
      <label>Amount<input type="number" step="0.01" required {...register('amount')} /></label>
      <label>Currency<input maxLength={3} required {...register('currency')} placeholder="USD" /></label>
      <label>Billing cycle<select required {...register('billingCycle')}><option value="WEEKLY">Weekly</option><option value="MONTHLY">Monthly</option><option value="QUARTERLY">Quarterly</option><option value="YEARLY">Yearly</option></select></label>
      <label>Start date<input type="date" required {...register('startDate')} /></label>
      <label>Next billing date<input type="date" {...register('nextBillingDate')} /></label>
      <label className="check-row"><input type="checkbox" {...register('reminderEnabled')} /> Renewal reminders enabled</label>
      <div className="span-two form-actions"><button type="button" className="button ghost" onClick={() => navigate('/subscriptions')}>Cancel</button><button className="button primary" disabled={isSubmitting}>Save subscription</button></div>
    </form>
  </>
}
