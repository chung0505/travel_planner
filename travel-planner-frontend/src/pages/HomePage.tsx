import { useState, useEffect } from 'react'
import Button from '../components/common/Button'
import TripCard from '../components/trip/TripCard'
import CreateTripModal from '../components/trip/CreateTripModal'
import { tripsApi } from '../api/trips'
import type { TripResponse } from '../types'

export default function HomePage() {
  const [trips, setTrips] = useState<TripResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)

  useEffect(() => {
    tripsApi.getAll()
      .then(setTrips)
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-3xl mx-auto px-4 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold text-gray-800">✈️ 旅遊行程規劃系統</h1>
            <p className="text-xs text-gray-400 mt-0.5">整合行程安排、景點管理與路線規劃</p>
          </div>
          <Button onClick={() => setShowCreate(true)}>+ 建立新行程</Button>
        </div>
      </header>

      <main className="max-w-3xl mx-auto px-4 py-6">
        {loading ? (
          <div className="flex justify-center py-16">
            <span className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
          </div>
        ) : trips.length === 0 ? (
          <div className="text-center py-20">
            <p className="text-5xl mb-4">🗺️</p>
            <p className="text-gray-500 font-medium">還沒有任何行程</p>
            <p className="text-gray-400 text-sm mt-1">點擊右上角「建立新行程」開始規劃你的旅程</p>
            <Button className="mt-6" onClick={() => setShowCreate(true)}>建立第一個行程</Button>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm text-gray-500">共 {trips.length} 個行程</p>
            {trips.map(trip => (
              <TripCard
                key={trip.id}
                trip={trip}
                onDeleted={id => setTrips(prev => prev.filter(t => t.id !== id))}
              />
            ))}
          </div>
        )}
      </main>

      {showCreate && (
        <CreateTripModal
          onClose={() => setShowCreate(false)}
          onCreated={trip => {
            setTrips(prev => [trip, ...prev])
            setShowCreate(false)
          }}
        />
      )}
    </div>
  )
}
