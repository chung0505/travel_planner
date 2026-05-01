import { useNavigate } from 'react-router-dom'
import type { TripResponse } from '../../types'

interface Props {
  trip: TripResponse
}

export default function TripCard({ trip }: Props) {
  const navigate = useNavigate()

  return (
    <div
      onClick={() => navigate(`/trips/${trip.id}`)}
      className="bg-white rounded-2xl border border-gray-200 p-5 cursor-pointer
        hover:shadow-md hover:border-blue-300 transition-all"
    >
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="text-lg font-bold text-gray-800">✈️ {trip.destination}</h3>
          <p className="text-sm text-gray-500 mt-1">
            {trip.departureDate} → {trip.returnDate}
          </p>
        </div>
        <div className="flex flex-col items-end gap-1">
          <span className="bg-blue-100 text-blue-700 text-xs font-medium px-2 py-1 rounded-full">
            {trip.totalDays} 天
          </span>
          <span className="text-xs text-gray-400">👥 {trip.companionCount} 人</span>
        </div>
      </div>

      <div className="mt-3 pt-3 border-t border-gray-100 flex gap-3 text-xs text-gray-400">
        <span>📅 {trip.totalDays} 個每日行程</span>
        <span>
          🗺️ {trip.dailyPlans.reduce((sum, d) => sum + d.attractions.length, 0)} 個景點
        </span>
      </div>
    </div>
  )
}
