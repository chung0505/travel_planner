import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import DailyPlanCard from '../components/dailyPlan/DailyPlanCard'
import RoutePlanner from '../components/route/RoutePlanner'
import { tripsApi } from '../api/trips'
import type { DailyPlanResponse, TripResponse } from '../types'

type Tab = 'plans' | 'route'

export default function TripDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const tripId = Number(id)

  const [trip, setTrip] = useState<TripResponse | null>(null)
  const [dailyPlans, setDailyPlans] = useState<DailyPlanResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<Tab>('plans')

  useEffect(() => {
    tripsApi.getById(tripId)
      .then(t => {
        setTrip(t)
        setDailyPlans(t.dailyPlans)
      })
      .catch(() => navigate('/'))
      .finally(() => setLoading(false))
  }, [tripId, navigate])

  const handlePlanUpdated = (updated: DailyPlanResponse) => {
    setDailyPlans(prev => prev.map(p => p.id === updated.id ? updated : p))
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <span className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
      </div>
    )
  }

  if (!trip) return null

  const totalAttractions = dailyPlans.reduce((sum, p) => sum + p.attractions.length, 0)

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-3xl mx-auto px-4 py-4">
          <button
            onClick={() => navigate('/')}
            className="text-sm text-gray-400 hover:text-gray-600 mb-3 flex items-center gap-1"
          >
            ← 返回行程列表
          </button>
          <div className="flex items-start justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold text-gray-800">✈️ {trip.destination}</h1>
              <p className="text-sm text-gray-500 mt-1">
                {trip.departureDate} → {trip.returnDate}
                <span className="mx-2 text-gray-300">|</span>
                {trip.totalDays} 天
                <span className="mx-2 text-gray-300">|</span>
                👥 {trip.companionCount} 人
              </p>
            </div>
            <div className="text-right text-xs text-gray-400 shrink-0">
              <p>{totalAttractions} 個景點</p>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="max-w-3xl mx-auto px-4">
          <div className="flex gap-1">
            {([
              { key: 'plans', label: '每日行程' },
              { key: 'route', label: '路線規劃' },
            ] as { key: Tab; label: string }[]).map(tab => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors
                  ${activeTab === tab.key
                    ? 'border-blue-600 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700'}`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>
      </header>

      <main className="max-w-3xl mx-auto px-4 py-6">
        {activeTab === 'plans' && (
          <div className="space-y-4">
            {dailyPlans.map(plan => (
              <DailyPlanCard
                key={plan.id}
                tripId={tripId}
                plan={plan}
                onPlanUpdated={handlePlanUpdated}
              />
            ))}
          </div>
        )}

        {activeTab === 'route' && (
          <div>
            <p className="text-sm text-gray-500 mb-4">
              從各日景點中勾選路線景點，選擇交通方式後取得估算，確認後儲存至行程。
            </p>
            <RoutePlanner tripId={tripId} dailyPlans={dailyPlans} />
          </div>
        )}
      </main>
    </div>
  )
}
