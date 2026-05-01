export type TransportationMethod = 'WALKING' | 'PUBLIC_TRANSIT' | 'TAXI' | 'SELF_DRIVING'

export const TRANSPORTATION_LABELS: Record<TransportationMethod, string> = {
  WALKING: '步行',
  PUBLIC_TRANSIT: '大眾運輸',
  TAXI: '計程車',
  SELF_DRIVING: '自駕',
}

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
  destination: string
  departureDate: string
  returnDate: string
  companionCount: number
  totalDays: number
  dailyPlans: DailyPlanResponse[]
}

export interface RouteSegmentResponse {
  fromAttraction: string
  toAttraction: string
  estimatedMinutes: number
  estimatedCost: number
}

export interface RouteEstimateResponse {
  attractionIds: number[]
  transportationMethod: TransportationMethod
  segments: RouteSegmentResponse[]
  totalEstimatedMinutes: number
  totalEstimatedCost: number
  geometry: [number, number][]  // [lat, lng] pairs
}

export interface RouteResponse {
  id: number
  tripId: number
  attractionIds: number[]
  transportationMethod: TransportationMethod
  estimatedDurationMinutes: number
  estimatedCost: number
  confirmed: boolean
  geometry: [number, number][]  // [lat, lng] pairs
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// Request types
export interface CreateTripRequest {
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
