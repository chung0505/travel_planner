import { api } from './client'
import type { AddExpenseRequest, BudgetSummaryResponse, SetBudgetRequest, SettlementResponse } from '../types'

export const budgetApi = {
  get: (tripId: number) =>
    api.get<BudgetSummaryResponse>(`/trips/${tripId}/budget`),

  set: (tripId: number, data: SetBudgetRequest) =>
    api.put<BudgetSummaryResponse>(`/trips/${tripId}/budget`, data),

  addExpense: (tripId: number, data: AddExpenseRequest) =>
    api.post<BudgetSummaryResponse>(`/trips/${tripId}/budget/expenses`, data),

  getSettlement: (tripId: number) =>
    api.get<SettlementResponse>(`/trips/${tripId}/budget/settlement`),
}
