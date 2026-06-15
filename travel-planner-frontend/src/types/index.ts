// ── Enums ────────────────────────────────────────────────────────────────────

export type TransportationMethod = 'WALKING' | 'PUBLIC_TRANSIT' | 'TAXI'
export type ExpenseType = 'ACCOMMODATION' | 'FOOD' | 'TRANSPORTATION' | 'TICKET' | 'OTHER'
export type ShareType = 'LINK' | 'SUMMARY'

export const TRANSPORTATION_LABELS: Record<TransportationMethod, string> = {
  WALKING: '步行',
  PUBLIC_TRANSIT: '大眾運輸',
  TAXI: '計程車',
}

export const EXPENSE_TYPE_LABELS: Record<ExpenseType, string> = {
  ACCOMMODATION: '住宿',
  FOOD: '餐飲',
  TRANSPORTATION: '交通',
  TICKET: '門票',
  OTHER: '其他',
}

export const EXPENSE_TYPE_ICONS: Record<ExpenseType, string> = {
  ACCOMMODATION: '🏨',
  FOOD: '🍽️',
  TRANSPORTATION: '🚌',
  TICKET: '🎟️',
  OTHER: '💳',
}

// ── Traveler ─────────────────────────────────────────────────────────────────

export interface TravelerResponse {
  id: number
  name: string
  email: string
}

// ── Trip / DailyPlan / Attraction ─────────────────────────────────────────────

export interface AttractionResponse {
  id: number
  name: string
  address: string
  startTime: string
  endTime: string
  latitude?: number
  longitude?: number
}

export interface DailyPlanResponse {
  id: number
  date: string
  dayNumber: number
  attractions: AttractionResponse[]
}

export interface TripResponse {
  id: number
  title: string
  destination: string
  departureDate: string
  returnDate: string
  companionCount: number
  totalDays: number
  dailyPlans: DailyPlanResponse[]
  organizer: TravelerResponse | null
  participants: TravelerResponse[]
}

// ── Route ─────────────────────────────────────────────────────────────────────

export interface TransitStepInfo {
  travelMode: 'TRANSIT' | 'WALKING'
  vehicleName: string
  lineName: string | null
  departureStop: string | null
  arrivalStop: string | null
  numStops: number | null
  durationMinutes: number
}

export interface RouteSegmentResponse {
  fromAttraction: string
  toAttraction: string
  estimatedMinutes: number
  estimatedCost: number
  transitSteps: TransitStepInfo[] | null
}

export interface RouteEstimateResponse {
  attractionIds: number[]
  transportationMethod: TransportationMethod
  segments: RouteSegmentResponse[]
  totalEstimatedMinutes: number
  totalEstimatedCost: number
  geometry: [number, number][]
}

export interface RouteResponse {
  id: number
  tripId: number
  attractionIds: number[]
  transportationMethod: TransportationMethod
  estimatedDurationMinutes: number
  estimatedCost: number
  confirmed: boolean
  geometry: [number, number][]
}

// ── Budget / Expense ───────────────────────────────────────────────────────────

export interface ExpenseSharingResponse {
  id: number
  travelerId: number
  travelerName: string
  amountPerPerson: number
}

export interface ExpenseResponse {
  id: number
  expenseType: ExpenseType
  amount: number
  date: string
  note: string | null
  currency: string
  exchangeRate: number
  originalAmount: number
  paidBy: string | null
  paidByTravelerId: number | null
  sharings: ExpenseSharingResponse[]
}

export interface BudgetSummaryResponse {
  budgetId: number
  tripId: number
  totalBudget: number
  currency: string
  totalSpent: number
  remainingBudget: number
  overBudget: boolean
  expenses: ExpenseResponse[]
}

// ── ShareLink ─────────────────────────────────────────────────────────────────

export interface ShareLinkResponse {
  id: number
  tripId: number
  token: string
  url: string
  shareType: ShareType
  assignedToTravelerId: number | null
  createdAt: string
  expiresAt: string
}

// ── API wrapper ───────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// ── Request types ─────────────────────────────────────────────────────────────

export interface CreateTripRequest {
  title: string
  destination: string
  departureDate: string
  returnDate: string
  companionCount: number
}

export interface AddAttractionRequest {
  name: string
  address: string
  startTime: string
  endTime: string
}

export interface PlanRouteRequest {
  attractionIds: number[]
  transportationMethod: TransportationMethod
}

export interface SetBudgetRequest {
  totalBudget: number
  currency: string
}

export interface AddExpenseRequest {
  expenseType: ExpenseType
  amount: number
  date: string
  note?: string
  currency: string
  exchangeRate: number
  originalAmount: number
  paidBy?: string
  paidByTravelerId?: number
  splitAmongTravelerIds?: number[]
  splitRatios?: Record<number, number>
}

// ── Settlement ────────────────────────────────────────────────────────────────

export interface TravelerBalance {
  travelerId: number
  travelerName: string
  totalPaid: number
  totalOwed: number
  balance: number
}

export interface TransferItem {
  fromTravelerId: number
  fromTravelerName: string
  toTravelerId: number
  toTravelerName: string
  amount: number
}

export interface SettlementResponse {
  balances: TravelerBalance[]
  transfers: TransferItem[]
}

export interface ShareItineraryRequest {
  shareType: ShareType
  assignedToTravelerId?: number
}
