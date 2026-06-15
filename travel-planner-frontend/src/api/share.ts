import { api } from './client'
import type { ShareItineraryRequest, ShareLinkResponse, TripResponse } from '../types'

export const shareApi = {
  share: (tripId: number, data: ShareItineraryRequest) =>
    api.post<ShareLinkResponse>(`/trips/${tripId}/share`, data),

  getByToken: (token: string) =>
    api.get<TripResponse>(`/share/${token}`),
}
