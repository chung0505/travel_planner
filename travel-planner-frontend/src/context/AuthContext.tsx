import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'
import type { TravelerResponse } from '../types'

interface AuthState {
  token: string | null
  traveler: TravelerResponse | null
}

interface AuthContextValue extends AuthState {
  login: (token: string, traveler: TravelerResponse) => void
  logout: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

const TOKEN_KEY = 'auth_token'
const TRAVELER_KEY = 'auth_traveler'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    const travelerJson = localStorage.getItem(TRAVELER_KEY)
    return {
      token,
      traveler: travelerJson ? JSON.parse(travelerJson) : null,
    }
  })

  const login = (token: string, traveler: TravelerResponse) => {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(TRAVELER_KEY, JSON.stringify(traveler))
    setState({ token, traveler })
  }

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(TRAVELER_KEY)
    setState({ token: null, traveler: null })
  }

  return (
    <AuthContext.Provider value={{ ...state, login, logout, isAuthenticated: !!state.token }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
