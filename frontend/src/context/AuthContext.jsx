import { createContext, useContext, useState } from 'react'
import api from '../api/axios'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('cloudnest_user')
    return stored ? JSON.parse(stored) : null
  })

  const login = async (username, password) => {
    const res = await api.post('/auth/login', { username, password })
    persist(res.data)
    return res.data
  }

  const register = async (payload) => {
    const res = await api.post('/auth/register', payload)
    persist(res.data)
    return res.data
  }

  const persist = (data) => {
    localStorage.setItem('cloudnest_token', data.token)
    localStorage.setItem('cloudnest_user', JSON.stringify(data))
    setUser(data)
  }

  const logout = () => {
    localStorage.removeItem('cloudnest_token')
    localStorage.removeItem('cloudnest_user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
