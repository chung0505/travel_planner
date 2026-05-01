import { api } from './client'
import type { CreateTripRequest, TripResponse } from '../types'

export const tripsApi = {
  getAll: () => api.get<TripResponse[]>('/trips'),
  getById: (id: number) => api.get<TripResponse>(`/trips/${id}`),
  create: (data: CreateTripRequest) => api.post<TripResponse>('/trips', data),
}
