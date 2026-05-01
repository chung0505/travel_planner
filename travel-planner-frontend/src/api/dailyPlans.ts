import { api } from './client'
import type { AddAttractionRequest, DailyPlanResponse } from '../types'

export const dailyPlansApi = {
  getAll: (tripId: number) => api.get<DailyPlanResponse[]>(`/trips/${tripId}/daily-plans`),
  addAttraction: (tripId: number, dailyPlanId: number, data: AddAttractionRequest) =>
    api.post<DailyPlanResponse>(`/trips/${tripId}/daily-plans/${dailyPlanId}/attractions`, data),
  removeAttraction: (tripId: number, dailyPlanId: number, attractionId: number) =>
    api.delete<void>(`/trips/${tripId}/daily-plans/${dailyPlanId}/attractions/${attractionId}`),
}
