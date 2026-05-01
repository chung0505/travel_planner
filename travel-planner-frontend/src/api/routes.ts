import { api } from './client'
import type { PlanRouteRequest, RouteEstimateResponse, RouteResponse } from '../types'

export const routesApi = {
  estimate: (tripId: number, data: PlanRouteRequest) =>
    api.post<RouteEstimateResponse>(`/trips/${tripId}/routes/estimate`, data),
  confirm: (tripId: number, data: PlanRouteRequest) =>
    api.post<RouteResponse>(`/trips/${tripId}/routes/confirm`, data),
  getAll: (tripId: number) => api.get<RouteResponse[]>(`/trips/${tripId}/routes`),
}
