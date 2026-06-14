import { api } from './client'
import type { PlanRouteRequest, RouteEstimateResponse, RouteResponse } from '../types'

export const routesApi = {
  estimate: (tripId: number, dailyPlanId: number, data: PlanRouteRequest) =>
    api.post<RouteEstimateResponse>(`/trips/${tripId}/daily-plans/${dailyPlanId}/routes/estimate`, data),
  confirm: (tripId: number, dailyPlanId: number, data: PlanRouteRequest) =>
    api.post<RouteResponse>(`/trips/${tripId}/daily-plans/${dailyPlanId}/routes/confirm`, data),
  getAll: (tripId: number, dailyPlanId: number) =>
    api.get<RouteResponse[]>(`/trips/${tripId}/daily-plans/${dailyPlanId}/routes`),
}
