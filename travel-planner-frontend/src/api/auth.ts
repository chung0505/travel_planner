import { api } from './client'
import type { TravelerResponse } from '../types'

interface AuthResponse {
  token: string
  traveler: TravelerResponse
}

interface LoginRequest {
  email: string
  password: string
}

interface RegisterRequest {
  name: string
  email: string
  password: string
}

export const authApi = {
  login: (data: LoginRequest) => api.post<AuthResponse>('/auth/login', data),
  register: (data: RegisterRequest) => api.post<AuthResponse>('/auth/register', data),
}
