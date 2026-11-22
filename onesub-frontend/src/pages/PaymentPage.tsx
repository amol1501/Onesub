import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { paymentApi, subscriptionApi } from '../api/modules'
import type { Payment, Subscription } from '../api/types'

export default function PaymentPage() {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([])
  const [subscriptionId, setSubscriptionId] = useState<number>(0)
  const [payments, setPayments] = useState<Payment[]>([])
  const [error, setError] = useState('')
  const { register, handleSubmit, reset } = useForm<Record<string, unknown>>()

  useEffect(() => {
    subscriptionApi.list(0, 100).then((page) => {
      setSubscriptions(page.content)
      if (page.content[0]) setSubscriptionId(page.content[0].id)
    }).catch((ex) => setError(ex.message))
  }, [])

  useEffect(() => {
    if (!subscriptionId) return
    paymentApi.list(subscriptionId).then((page) => setPayments(page.content)).catch((ex) => setError(ex.message))
  }, [subscriptionId])

  const submit = handleSubmit(async (values) => {
    try {
      await paymentApi.create(subscriptionId, { ...values, amount: Number(values.amount) })
      reset()
      const page = await paymentApi.list(subscriptionId)
      setPayments(page.content)
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'Unable to record payment')
    }
  })

  return <>
    <div className="page-header"><div><h1>Payment History</h1><p>Manually recorded payments only</p></div></div>
    {error && <div className="error">{error}</div>}
    <div className="panel form-inline"><label>Subscription<select value={subscriptionId} onChange={(event) => setSubscriptionId(Number(event.target.value))}>{subscriptions.map((item) => <option key={item.id} value={item.id}>{item.merchantName}</option>)}</select></label></div>
    {subscriptionId > 0 && <form className="panel form-grid" onSubmit={submit}><label>Amount<input type="number" step="0.01" required {...register('amount')} /></label><label>Currency<input required maxLength={3} {...register('currency')} /></label><label>Payment date<input type="date" required {...register('paymentDate')} /></label><label>Status<select required {...register('paymentStatus')}><option value="PAID">Paid</option><option value="PENDING">Pending</option><option value="FAILED">Failed</option></select></label><label className="span-two">Notes<textarea rows={3} {...register('notes')} /></label><div className="span-two form-actions"><button className="button primary">Record payment</button></div></form>}
    <div className="panel table-wrap"><table><thead><tr><th>Date</th><th>Amount</th><th>Status</th><th>Notes</th></tr></thead><tbody>{payments.map((payment) => <tr key={payment.id}><td>{payment.paymentDate}</td><td>{payment.amount} {payment.currency}</td><td><span className={`status ${payment.paymentStatus.toLowerCase()}`}>{payment.paymentStatus}</span></td><td>{payment.notes || '-'}</td></tr>)}</tbody></table>{!payments.length && <div className="empty">No payment records</div>}</div>
  </>
}
