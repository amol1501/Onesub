import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const { register, handleSubmit, formState: { isSubmitting } } = useForm<{email: string; password: string}>()

  const submit = handleSubmit(async (values) => {
    setError('')
    try {
      await login(values.email, values.password)
      navigate('/dashboard')
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'Login failed')
    }
  })

  return <AuthCard title="Welcome back">
    <form onSubmit={submit} className="form">
      {error && <div className="error">{error}</div>}
      <label>Email<input type="email" required {...register('email')} /></label>
      <label>Password<input type="password" required {...register('password')} /></label>
      <button className="button primary" disabled={isSubmitting}>Sign in</button>
    </form>
    <p>New to OneSub? <Link to="/register">Create an account</Link></p>
  </AuthCard>
}

export function RegisterPage() {
  const { register: registerUser } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const { register, handleSubmit, formState: { isSubmitting } } =
    useForm<{fullName: string; email: string; password: string}>()

  const submit = handleSubmit(async (values) => {
    setError('')
    try {
      await registerUser(values.fullName, values.email, values.password)
      navigate('/login')
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : 'Registration failed')
    }
  })

  return <AuthCard title="Create your account">
    <form onSubmit={submit} className="form">
      {error && <div className="error">{error}</div>}
      <label>Full name<input required {...register('fullName')} /></label>
      <label>Email<input type="email" required {...register('email')} /></label>
      <label>Password<input type="password" required minLength={8} {...register('password')} /></label>
      <button className="button primary" disabled={isSubmitting}>Create account</button>
    </form>
    <p>Already have an account? <Link to="/login">Sign in</Link></p>
  </AuthCard>
}

function AuthCard({ title, children }: { title: string; children: React.ReactNode }) {
  return <div className="auth-page"><div className="auth-card"><div className="brand"><span className="brand-mark">O</span> OneSub</div><h1>{title}</h1>{children}</div></div>
}
