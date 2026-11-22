import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { merchantApi } from '../api/modules'
import type { Merchant } from '../api/types'

export default function MerchantPage() {
  const [merchants, setMerchants] = useState<Merchant[]>([])
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const { register, handleSubmit, reset } = useForm<{name: string; category: string; websiteUrl: string}>()

  const load = () => merchantApi.list(0, 100).then((page) => setMerchants(page.content)).catch((ex) => setError(ex.message))
  useEffect(() => { void load() }, [])

  const submit = handleSubmit(async (values) => {
    try {
      await merchantApi.create({ ...values, isActive: true })
      reset()
      setShowForm(false)
      await load()
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'Unable to add merchant')
    }
  })

  return <>
    <div className="page-header"><div><h1>Merchants</h1><p>Global catalogue and your custom merchants</p></div><button className="button primary" onClick={() => setShowForm((value) => !value)}>Add custom merchant</button></div>
    {error && <div className="error">{error}</div>}
    {showForm && <form className="panel form-grid" onSubmit={submit}><label>Merchant name<input required {...register('name')} /></label><label>Category<input {...register('category')} /></label><label>Website<input type="url" {...register('websiteUrl')} /></label><div className="span-two form-actions"><button type="button" className="button ghost" onClick={() => setShowForm(false)}>Cancel</button><button className="button primary">Save merchant</button></div></form>}
    <div className="merchant-grid">{merchants.map((merchant) => <div className="merchant-card" key={merchant.id}><div className="merchant-logo">{merchant.name.slice(0, 1)}</div><div><strong>{merchant.name}</strong><small>{merchant.category || 'Custom merchant'}</small></div><span className={merchant.custom ? 'tag custom' : 'tag'}>{merchant.custom ? 'Custom' : 'Catalogue'}</span></div>)}</div>
  </>
}
