import { api } from './client'
import type { TravelerResponse } from '../types'

export interface CreateTravelerRequest {
  name: string
  email: string
  password: string
}

export const travelersApi = {
  getAll: () => api.get<TravelerResponse[]>('/travelers'),
  getById: (id: number) => api.get<TravelerResponse>(`/travelers/${id}`),
  create: (data: CreateTravelerRequest) => api.post<TravelerResponse>('/travelers', data),
}
