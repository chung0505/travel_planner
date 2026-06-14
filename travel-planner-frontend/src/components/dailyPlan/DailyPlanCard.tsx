import { useState, useEffect } from 'react'
import Button from '../common/Button'
import AddAttractionModal from './AddAttractionModal'
import MapView from '../route/MapView'
import { dailyPlansApi } from '../../api/dailyPlans'
import { routesApi } from '../../api/routes'
import type {
  AttractionResponse,
  DailyPlanResponse,
  RouteEstimateResponse,
  TransitStepInfo,
} from '../../types'
import { TRANSPORTATION_LABELS } from '../../types'

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

const TRANSPORT_ICONS: Record<string, string> = {
  WALKING: '🚶',
  PUBLIC_TRANSIT: '🚌',
  TAXI: '🚕',
}

function DayRoutePanel({
  tripId,
  plan,
}: {
  tripId: number
  plan: DailyPlanResponse
}) {
  const [loading, setLoading] = useState(true)
  const [estimates, setEstimates] = useState<RouteEstimateResponse[]>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const routes = await routesApi.getAll(tripId, plan.id)
        const dayAttractionIds = new Set(plan.attractions.map(a => a.id))
        const attractionOrder = new Map(plan.attractions.map((a, i) => [a.id, i]))

        // 取得所有屬於這一天景點的已儲存路線
        const matched = routes.filter(
          r =>
            r.attractionIds.length >= 2 &&
            r.attractionIds.every(id => dayAttractionIds.has(id)),
        )

        if (matched.length === 0) {
          setError('這一天尚無已儲存的路線，請至「路線規劃」頁面規劃並儲存後再查看。')
          return
        }

        // 依照第一個景點在當天的順序排序
        matched.sort((a, b) => {
          const aIdx = attractionOrder.get(a.attractionIds[0]) ?? 0
          const bIdx = attractionOrder.get(b.attractionIds[0]) ?? 0
          return aIdx - bIdx
        })

        // 並行取得所有路線的完整估算（含 segments）
        const ests = await Promise.all(
          matched.map(r =>
            routesApi.estimate(tripId, plan.id, {
              attractionIds: r.attractionIds,
              transportationMethod: r.transportationMethod,
            }),
          ),
        )
        setEstimates(ests)
      } catch {
        setError('載入路線失敗，請稍後再試')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [tripId, plan])

  const fmtMin = (min: number) =>
    min >= 60 ? `${Math.floor(min / 60)} 小時 ${min % 60} 分` : `${min} 分鐘`

  if (loading) {
    return (
      <div className="px-4 py-6 border-t border-gray-100 flex justify-center">
        <span className="w-5 h-5 border-2 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="px-4 py-4 border-t border-gray-100 bg-gray-50/50">
        <p className="text-xs text-gray-400 text-center">{error}</p>
      </div>
    )
  }

  if (estimates.length === 0) return null

  // 合併所有路線的 geometry（去除銜接點重複）與 segments
  const mergedGeometry = estimates.flatMap((est, i) =>
    i === 0 ? est.geometry : est.geometry.slice(1),
  )
  const allAttractions = estimates.flatMap(est =>
    est.attractionIds
      .map(id => plan.attractions.find(a => a.id === id))
      .filter((a): a is AttractionResponse => a != null),
  )
  // 相鄰路線銜接點去重
  const uniqueAttractions = allAttractions.filter(
    (a, i) => i === 0 || a.id !== allAttractions[i - 1].id,
  )
  const totalMinutes = estimates.reduce((s, e) => s + e.totalEstimatedMinutes, 0)
  const totalCost = estimates.reduce((s, e) => s + e.totalEstimatedCost, 0)

  // 全部 segments 合併，並加上全域編號
  const allSegments = estimates.flatMap(est =>
    est.segments.map(seg => ({ ...seg, transportationMethod: est.transportationMethod })),
  )

  return (
    <div className="px-4 py-4 border-t border-gray-100 bg-gray-50/50 space-y-4">
      {/* 摘要列 */}
      <div className="flex items-center gap-3 text-xs text-gray-600 flex-wrap">
        <span>總時間：<strong>{fmtMin(totalMinutes)}</strong></span>
        {totalCost > 0 && <span>總費用：<strong>NT$ {totalCost}</strong></span>}
      </div>

      {/* 合併地圖 */}
      <MapView geometry={mergedGeometry} attractions={uniqueAttractions} />

      {/* 所有路段 */}
      <div className="space-y-2">
        {allSegments.map((seg, i) => (
          <div
            key={i}
            className="bg-white rounded-lg border border-gray-100 overflow-hidden"
          >
            {/* 起訖景點 + 總覽 */}
            <div className="flex items-start gap-3 px-3 py-2.5">
              <div className="flex flex-col items-center mt-0.5 shrink-0">
                <span className="w-5 h-5 rounded-full bg-blue-600 text-white text-xs font-bold flex items-center justify-center">
                  {i + 1}
                </span>
                <div className="w-px bg-blue-200 my-1" style={{ minHeight: '14px' }} />
                <span className="w-5 h-5 rounded-full bg-blue-400 text-white text-xs font-bold flex items-center justify-center">
                  {i + 2}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs font-medium text-gray-800 truncate">{seg.fromAttraction}</p>
                <div className="flex items-center gap-2 my-1 text-xs text-gray-400">
                  <span>{TRANSPORT_ICONS[seg.transportationMethod]}</span>
                  <span className="text-blue-500 font-medium">{fmtMin(seg.estimatedMinutes)}</span>
                  {seg.estimatedCost > 0 && <span>NT$ {seg.estimatedCost}</span>}
                </div>
                <p className="text-xs font-medium text-gray-800 truncate">{seg.toAttraction}</p>
              </div>
            </div>

            {/* Transit 步驟細節 */}
            {seg.transitSteps && seg.transitSteps.length > 0 && (
              <div className="border-t border-gray-50 px-3 py-2 space-y-1.5 bg-blue-50/40">
                {seg.transitSteps.map((step: TransitStepInfo, si: number) => (
                  <div key={si} className="flex items-center gap-2 text-xs">
                    {step.travelMode === 'TRANSIT' ? (
                      <>
                        <span className="text-blue-600 font-medium shrink-0">
                          {step.vehicleName === '捷運' ? '🚇' : '🚌'} {step.vehicleName}
                          {step.lineName ? ` ${step.lineName}` : ''}
                        </span>
                        <span className="text-gray-400 shrink-0">
                          {step.departureStop} → {step.arrivalStop}
                        </span>
                        {step.numStops != null && step.numStops > 0 && (
                          <span className="text-gray-300 shrink-0">{step.numStops} 站</span>
                        )}
                        <span className="text-gray-300 shrink-0 ml-auto">{fmtMin(step.durationMinutes)}</span>
                      </>
                    ) : (
                      <>
                        <span className="text-gray-400 shrink-0">🚶 步行</span>
                        <span className="text-gray-300 shrink-0 ml-auto">{fmtMin(step.durationMinutes)}</span>
                      </>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

export default function DailyPlanCard({ tripId, plan, onPlanUpdated }: Props) {
  const [showModal, setShowModal] = useState(false)
  const [showRoute, setShowRoute] = useState(false)

  const handleRemoved = (attractionId: number) => {
    onPlanUpdated({
      ...plan,
      attractions: plan.attractions.filter(a => a.id !== attractionId),
    })
    setShowRoute(false)
  }

  const canShowRoute = plan.attractions.length >= 2

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
          <div className="flex items-center gap-2">
            {canShowRoute && (
              <button
                onClick={() => setShowRoute(v => !v)}
                className={`text-xs px-3 py-1 rounded-full border transition-colors font-medium
                  ${showRoute
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'text-blue-600 border-blue-200 hover:bg-blue-50'
                  }`}
              >
                {showRoute ? '收起路線' : '查看路線'}
              </button>
            )}
            <Button variant="ghost" onClick={() => setShowModal(true)} className="text-xs py-1">
              + 新增景點
            </Button>
          </div>
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

        {showRoute && <DayRoutePanel tripId={tripId} plan={plan} />}
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
