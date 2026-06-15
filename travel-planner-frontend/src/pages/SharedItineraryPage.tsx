import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { shareApi } from '../api/share'
import type { TripResponse } from '../types'

export default function SharedItineraryPage() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()
  const [trip, setTrip] = useState<TripResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) { navigate('/'); return }
    shareApi.getByToken(token)
      .then(setTrip)
      .catch(err => setError(err instanceof Error ? err.message : '無法載入行程'))
      .finally(() => setLoading(false))
  }, [token, navigate])

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <span className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
      </div>
    )
  }

  if (error || !trip) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <p className="text-5xl mb-4">🔗</p>
          <p className="text-gray-700 font-medium text-lg">分享連結已失效</p>
          <p className="text-gray-400 text-sm mt-1">{error || '此連結可能已過期或不存在'}</p>
        </div>
      </div>
    )
  }

  const totalAttractions = trip.dailyPlans.reduce((sum, p) => sum + p.attractions.length, 0)

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-2xl mx-auto px-4 py-4">
          <p className="text-xs text-gray-400 mb-2 flex items-center gap-1">
            <span>🔗</span> 分享的行程（唯讀）
          </p>
          <h1 className="text-2xl font-bold text-gray-800">{trip.title}</h1>
          <p className="text-sm text-gray-500 mt-0.5">✈️ {trip.destination}</p>
          <p className="text-sm text-gray-400 mt-1">
            {trip.departureDate} → {trip.returnDate}
            <span className="mx-2 text-gray-300">|</span>
            {trip.totalDays} 天
            <span className="mx-2 text-gray-300">|</span>
            👥 {trip.companionCount} 人
            <span className="mx-2 text-gray-300">|</span>
            🗺️ {totalAttractions} 個景點
          </p>
        </div>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-6 space-y-4">
        {trip.dailyPlans.map(plan => (
          <div key={plan.id} className="bg-white rounded-2xl border border-gray-200 overflow-hidden">
            <div className="px-5 py-3 bg-gray-50 border-b border-gray-100 flex items-center gap-3">
              <span className="w-7 h-7 rounded-full bg-blue-600 text-white text-xs font-bold flex items-center justify-center shrink-0">
                {plan.dayNumber}
              </span>
              <div>
                <p className="text-sm font-semibold text-gray-700">第 {plan.dayNumber} 天</p>
                <p className="text-xs text-gray-400">{plan.date}</p>
              </div>
            </div>

            {plan.attractions.length === 0 ? (
              <p className="px-5 py-4 text-sm text-gray-400">尚無景點安排</p>
            ) : (
              <ul className="divide-y divide-gray-100">
                {plan.attractions.map((attraction, idx) => (
                  <li key={attraction.id} className="px-5 py-4 flex items-start gap-3">
                    <span className="w-5 h-5 rounded-full bg-blue-100 text-blue-600 text-xs font-semibold flex items-center justify-center shrink-0 mt-0.5">
                      {idx + 1}
                    </span>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-800">{attraction.name}</p>
                      <p className="text-xs text-gray-400 mt-0.5">{attraction.address}</p>
                    </div>
                    <p className="text-xs text-gray-500 shrink-0 mt-0.5">
                      {attraction.startTime} – {attraction.endTime}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </div>
        ))}
      </main>
    </div>
  )
}
