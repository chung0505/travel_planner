import { useState } from 'react'
import Button from '../common/Button'
import AddAttractionModal from './AddAttractionModal'
import { dailyPlansApi } from '../../api/dailyPlans'
import type { AttractionResponse, DailyPlanResponse } from '../../types'

interface Props {
  tripId: number
  plan: DailyPlanResponse
  onPlanUpdated: (plan: DailyPlanResponse) => void
}

function AttractionRow({
  attraction,
  tripId,
  dailyPlanId,
  onRemoved,
}: {
  attraction: AttractionResponse
  tripId: number
  dailyPlanId: number
  onRemoved: (id: number) => void
}) {
  const [removing, setRemoving] = useState(false)

  const handleRemove = async () => {
    setRemoving(true)
    try {
      await dailyPlansApi.removeAttraction(tripId, dailyPlanId, attraction.id)
      onRemoved(attraction.id)
    } catch {
      setRemoving(false)
    }
  }

  const fmt = (t: string) => t.slice(0, 5)

  return (
    <div className="flex items-center gap-3 py-2 px-3 rounded-lg hover:bg-gray-50 group">
      <div className="text-xs text-gray-400 font-mono w-20 shrink-0">
        {fmt(attraction.startTime)} – {fmt(attraction.endTime)}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-800 truncate">{attraction.name}</p>
        <p className="text-xs text-gray-400 truncate">{attraction.address}</p>
      </div>
      <button
        onClick={handleRemove}
        disabled={removing}
        className="opacity-0 group-hover:opacity-100 text-gray-300 hover:text-red-400
          transition-opacity text-lg leading-none disabled:cursor-not-allowed"
      >
        {removing ? '…' : '×'}
      </button>
    </div>
  )
}

export default function DailyPlanCard({ tripId, plan, onPlanUpdated }: Props) {
  const [showModal, setShowModal] = useState(false)

  const handleRemoved = (attractionId: number) => {
    onPlanUpdated({
      ...plan,
      attractions: plan.attractions.filter(a => a.id !== attractionId),
    })
  }

  return (
    <>
      <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden">
        <div className="flex items-center justify-between px-4 py-3 bg-gray-50 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <span className="bg-blue-600 text-white text-xs font-bold w-6 h-6 rounded-full flex items-center justify-center">
              {plan.dayNumber}
            </span>
            <span className="text-sm font-semibold text-gray-700">第 {plan.dayNumber} 天</span>
            <span className="text-xs text-gray-400">{plan.date}</span>
          </div>
          <Button variant="ghost" onClick={() => setShowModal(true)} className="text-xs py-1">
            + 新增景點
          </Button>
        </div>

        <div className="divide-y divide-gray-50">
          {plan.attractions.length === 0 ? (
            <p className="text-center text-sm text-gray-400 py-6">尚無景點，點擊右上角新增</p>
          ) : (
            plan.attractions.map(a => (
              <AttractionRow
                key={a.id}
                attraction={a}
                tripId={tripId}
                dailyPlanId={plan.id}
                onRemoved={handleRemoved}
              />
            ))
          )}
        </div>
      </div>

      {showModal && (
        <AddAttractionModal
          tripId={tripId}
          dailyPlanId={plan.id}
          date={plan.date}
          onClose={() => setShowModal(false)}
          onAdded={updated => {
            onPlanUpdated(updated)
            setShowModal(false)
          }}
        />
      )}
    </>
  )
}
