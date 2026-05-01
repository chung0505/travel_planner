import { useState } from 'react'
import Button from '../common/Button'
import MapView from './MapView'
import { routesApi } from '../../api/routes'
import { TRANSPORTATION_LABELS, type AttractionResponse, type DailyPlanResponse, type RouteEstimateResponse, type RouteResponse, type TransportationMethod } from '../../types'

interface Props {
  tripId: number
  dailyPlans: DailyPlanResponse[]
}

const TRANSPORT_METHODS: TransportationMethod[] = ['WALKING', 'PUBLIC_TRANSIT', 'TAXI', 'SELF_DRIVING']

const TRANSPORT_ICON: Record<TransportationMethod, string> = {
  WALKING: '🚶',
  PUBLIC_TRANSIT: '🚌',
  TAXI: '🚕',
  SELF_DRIVING: '🚗',
}

function allAttractions(dailyPlans: DailyPlanResponse[]): (AttractionResponse & { date: string })[] {
  return dailyPlans.flatMap(p => p.attractions.map(a => ({ ...a, date: p.date })))
}

export default function RoutePlanner({ tripId, dailyPlans }: Props) {
  const attractions = allAttractions(dailyPlans)

  const [selected, setSelected] = useState<number[]>([])
  const [method, setMethod] = useState<TransportationMethod>('PUBLIC_TRANSIT')
  const [estimate, setEstimate] = useState<RouteEstimateResponse | null>(null)
  const [confirmed, setConfirmed] = useState<RouteResponse[]>([])
  const [estimating, setEstimating] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [error, setError] = useState('')

  const toggle = (id: number) =>
    setSelected(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    )

  const handleEstimate = async () => {
    if (selected.length < 2) { setError('請至少選擇兩個景點'); return }
    setError('')
    setEstimating(true)
    try {
      const result = await routesApi.estimate(tripId, { attractionIds: selected, transportationMethod: method })
      setEstimate(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : '估算失敗')
    } finally {
      setEstimating(false)
    }
  }

  const handleConfirm = async () => {
    if (!estimate) return
    setConfirming(true)
    setError('')
    try {
      const route = await routesApi.confirm(tripId, { attractionIds: selected, transportationMethod: method })
      setConfirmed(prev => [...prev, route])
      setEstimate(null)
      setSelected([])
    } catch (err) {
      setError(err instanceof Error ? err.message : '確認失敗')
    } finally {
      setConfirming(false)
    }
  }

  const fmtMinutes = (min: number) =>
    min >= 60 ? `${Math.floor(min / 60)} 小時 ${min % 60} 分` : `${min} 分`

  if (attractions.length === 0) {
    return (
      <div className="bg-white rounded-2xl border border-dashed border-gray-200 p-8 text-center text-gray-400 text-sm">
        請先在每日行程中新增至少兩個景點，再進行路線規劃。
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Step 1 - Select attractions */}
      <div className="bg-white rounded-2xl border border-gray-200 p-4">
        <p className="text-sm font-semibold text-gray-700 mb-3">① 選擇景點（按順序勾選）</p>
        <div className="space-y-1">
          {attractions.map((a) => (
            <label
              key={a.id}
              className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer"
            >
              <input
                type="checkbox"
                checked={selected.includes(a.id)}
                onChange={() => toggle(a.id)}
                className="w-4 h-4 accent-blue-600"
              />
              <span className="text-xs text-gray-400 w-5 text-center font-mono">
                {selected.includes(a.id) ? selected.indexOf(a.id) + 1 : ''}
              </span>
              <div className="flex-1 min-w-0">
                <span className="text-sm font-medium text-gray-800">{a.name}</span>
                <span className="text-xs text-gray-400 ml-2">{a.date}</span>
              </div>
              <span className="text-xs text-gray-400 font-mono">
                {a.startTime.slice(0, 5)}
              </span>
            </label>
          ))}
        </div>
      </div>

      {/* Step 2 - Choose transport */}
      <div className="bg-white rounded-2xl border border-gray-200 p-4">
        <p className="text-sm font-semibold text-gray-700 mb-3">② 選擇交通方式</p>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {TRANSPORT_METHODS.map(m => (
            <button
              key={m}
              onClick={() => setMethod(m)}
              className={`flex flex-col items-center gap-1 p-3 rounded-xl border text-sm transition-all
                ${method === m
                  ? 'border-blue-500 bg-blue-50 text-blue-700 font-medium'
                  : 'border-gray-200 text-gray-600 hover:border-gray-300'}`}
            >
              <span className="text-xl">{TRANSPORT_ICON[m]}</span>
              {TRANSPORTATION_LABELS[m]}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <p className="text-sm text-red-500 bg-red-50 rounded-xl px-4 py-2">⚠️ {error}</p>
      )}

      {/* Step 3 - Estimate */}
      {!estimate ? (
        <Button onClick={handleEstimate} loading={estimating} className="w-full justify-center py-3">
          🔍 計算路線估算
        </Button>
      ) : (
        <div className="bg-white rounded-2xl border border-blue-200 p-4 space-y-3">
          <p className="text-sm font-semibold text-gray-700">路線估算結果</p>

          {estimate.geometry?.length > 1 && (
            <MapView
              geometry={estimate.geometry}
              attractions={attractions.filter(a => selected.includes(a.id))}
            />
          )}

          <div className="space-y-2">
            {estimate.segments.map((seg, idx) => (
              <div key={idx} className="flex items-center gap-2 text-sm">
                <span className="text-gray-700 font-medium">{seg.fromAttraction}</span>
                <span className="text-gray-300 flex-1 border-t border-dashed border-gray-200 mx-1" />
                <span className="text-xs text-gray-500">{fmtMinutes(seg.estimatedMinutes)}</span>
                <span className="text-xs text-gray-400">NT${seg.estimatedCost}</span>
                <span className="text-gray-700 font-medium">{seg.toAttraction}</span>
              </div>
            ))}
          </div>

          <div className="flex items-center justify-between pt-3 border-t border-gray-100 text-sm">
            <span className="text-gray-500">
              合計：{fmtMinutes(estimate.totalEstimatedMinutes)} ／ NT${estimate.totalEstimatedCost}
            </span>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => setEstimate(null)}>重新規劃</Button>
              <Button onClick={handleConfirm} loading={confirming}>確認儲存路線</Button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmed routes */}
      {confirmed.length > 0 && (
        <div className="space-y-4">
          <p className="text-sm font-semibold text-gray-600">已確認的路線</p>
          {confirmed.map(r => (
            <div key={r.id} className="bg-green-50 border border-green-200 rounded-xl p-4 text-sm space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-medium text-green-800">
                  {TRANSPORT_ICON[r.transportationMethod]} {TRANSPORTATION_LABELS[r.transportationMethod]}
                  ・{r.attractionIds.length} 個景點
                </span>
                <span className="text-green-600 text-xs">
                  {fmtMinutes(r.estimatedDurationMinutes)} ／ NT${r.estimatedCost}
                </span>
              </div>
              {r.geometry?.length > 1 && (
                <MapView
                  geometry={r.geometry}
                  attractions={attractions.filter(a => r.attractionIds.includes(a.id))}
                />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
