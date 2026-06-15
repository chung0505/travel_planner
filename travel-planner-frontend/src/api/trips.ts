import { api } from './client'
import type { CreateTripRequest, TripResponse } from '../types'

export const tripsApi = {
  getAll: () => api.get<TripResponse[]>('/trips'),
  getById: (id: number) => api.get<TripResponse>(`/trips/${id}`),
  create: (data: CreateTripRequest) => api.post<TripResponse>('/trips', data),
  delete: (id: number) => api.delete<void>(`/trips/${id}`),
  addParticipant: (tripId: number, travelerId: number) =>
    api.post<TripResponse>(`/trips/${tripId}/participants/${travelerId}`, {}),
  removeParticipant: (tripId: number, travelerId: number) =>
    api.delete<TripResponse>(`/trips/${tripId}/participants/${travelerId}`),
}
