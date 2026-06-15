import { useState } from 'react'
import { tripsApi } from '../../api/trips'
import { travelersApi } from '../../api/travelers'
import type { TripResponse, TravelerResponse } from '../../types'
import { useAuth } from '../../context/AuthContext'

interface Props {
  trip: TripResponse
  onUpdated: (trip: TripResponse) => void
}

export default function ParticipantsPanel({ trip, onUpdated }: Props) {
  const { traveler: me } = useAuth()
  const [email, setEmail] = useState('')
  const [searching, setSearching] = useState(false)
  const [found, setFound] = useState<TravelerResponse | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState('')

  // organizer + participants 合併顯示（去重）
  const allMembers: TravelerResponse[] = []
  if (trip.organizer) allMembers.push(trip.organizer)
  trip.participants.forEach(p => {
    if (!allMembers.find(m => m.id === p.id)) allMembers.push(p)
  })

  const isFull = allMembers.length >= trip.companionCount

  const handleSearch = async () => {
    if (!email.trim()) return
    setSearching(true)
    setFound(null)
    setNotFound(false)
    setError('')
    try {
      const all = await travelersApi.getAll()
      const match = all.find(t => t.email.toLowerCase() === email.trim().toLowerCase())
      if (match) {
        setFound(match)
      } else {
        setNotFound(true)
      }
    } catch {
      setError('搜尋失敗')
    } finally {
      setSearching(false)
    }
  }

  const handleAdd = async (traveler: TravelerResponse) => {
    try {
      const updated = await tripsApi.addParticipant(trip.id, traveler.id)
      onUpdated(updated)
      setFound(null)
      setEmail('')
    } catch (err) {
      setError(err instanceof Error ? err.message : '新增失敗')
    }
  }

  const handleRemove = async (travelerId: number) => {
    try {
      const updated = await tripsApi.removeParticipant(trip.id, travelerId)
      onUpdated(updated)
    } catch (err) {
      setError(err instanceof Error ? err.message : '移除失敗')
    }
  }

  const isAlreadyMember = (id: number) => allMembers.some(m => m.id === id)

  return (
    <div className="space-y-4">
      {/* 目前成員 */}
      <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-800">旅程成員</h3>
        </div>
        <ul className="divide-y divide-gray-100">
          {allMembers.map(m => (
            <li key={m.id} className="flex items-center justify-between px-5 py-3">
              <div>
                <p className="text-sm font-medium text-gray-800">{m.name}</p>
                <p className="text-xs text-gray-400">{m.email}</p>
              </div>
              <div className="flex items-center gap-2">
                {m.id === trip.organizer?.id && (
                  <span className="text-xs bg-blue-100 text-blue-600 px-2 py-0.5 rounded-full">建立者</span>
                )}
                {m.id !== trip.organizer?.id && me?.id !== m.id && (
                  <button
                    onClick={() => handleRemove(m.id)}
                    className="text-xs text-red-400 hover:text-red-600 transition-colors"
                  >
                    移除
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      </div>

      {/* 新增旅伴 */}
      <div className="bg-white rounded-2xl border border-gray-200 p-5">
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-semibold text-gray-800">新增旅伴</h3>
          <span className="text-xs text-gray-400">{allMembers.length} / {trip.companionCount} 人</span>
        </div>

        {isFull ? (
          <div className="bg-amber-50 text-amber-700 text-sm rounded-lg px-4 py-3">
            已達行程人數上限（{trip.companionCount} 人），無法再新增旅伴。
            若需增加人數，請調整行程設定。
          </div>
        ) : (
          <>
            <p className="text-xs text-gray-400 mb-3">輸入旅伴的帳號 Email 來搜尋並加入</p>
            <div className="flex gap-2">
              <input
                type="email"
                value={email}
                onChange={e => { setEmail(e.target.value); setFound(null); setNotFound(false) }}
                onKeyDown={e => e.key === 'Enter' && handleSearch()}
                placeholder="旅伴的 Email"
                className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
              />
              <button
                onClick={handleSearch}
                disabled={searching || !email.trim()}
                className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
              >
                {searching ? '搜尋中…' : '搜尋'}
              </button>
            </div>
          </>
        )}

        {!isFull && notFound && (
          <p className="mt-2 text-sm text-gray-500">找不到此 Email 的旅客，請確認對方是否已註冊</p>
        )}

        {!isFull && found && (
          <div className="mt-3 flex items-center justify-between bg-gray-50 rounded-lg px-4 py-3">
            <div>
              <p className="text-sm font-medium text-gray-800">{found.name}</p>
              <p className="text-xs text-gray-400">{found.email}</p>
            </div>
            {isAlreadyMember(found.id) ? (
              <span className="text-xs text-gray-400">已在行程中</span>
            ) : (
              <button
                onClick={() => handleAdd(found)}
                className="text-sm text-blue-600 font-medium hover:text-blue-800 transition-colors"
              >
                加入
              </button>
            )}
          </div>
        )}

        {error && <p className="mt-2 text-sm text-red-500">{error}</p>}
      </div>
    </div>
  )
}
