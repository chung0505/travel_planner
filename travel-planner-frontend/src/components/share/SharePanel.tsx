import { useState } from 'react'
import Button from '../common/Button'
import { shareApi } from '../../api/share'
import type { ShareLinkResponse, ShareType, TripResponse } from '../../types'

interface Props {
  tripId: number
  trip: TripResponse
}

export default function SharePanel({ tripId, trip }: Props) {
  const [selected, setSelected] = useState<ShareType>('LINK')
  const [sharing, setSharing] = useState(false)
  const [linkResult, setLinkResult] = useState<ShareLinkResponse | null>(null)
  const [summaryText, setSummaryText] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState('')

  const handleSelect = (type: ShareType) => {
    setSelected(type)
    setLinkResult(null)
    setSummaryText(null)
    setCopied(false)
    setError('')
  }

  const handleGenerateLink = async () => {
    setSharing(true)
    setError('')
    setLinkResult(null)
    try {
      const res = await shareApi.share(tripId, { shareType: 'LINK' })
      setLinkResult(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : '產生分享連結失敗')
    } finally {
      setSharing(false)
    }
  }

  const handleGenerateSummary = () => {
    const lines: string[] = []
    lines.push(`✈️ ${trip.title}`)
    lines.push(`目的地：${trip.destination}`)
    lines.push(`日期：${trip.departureDate} → ${trip.returnDate}（${trip.totalDays} 天）`)
    lines.push(`人數：${trip.companionCount} 人`)

    for (const plan of trip.dailyPlans) {
      lines.push('')
      lines.push(`📅 第 ${plan.dayNumber} 天（${plan.date}）`)
      if (plan.attractions.length === 0) {
        lines.push('  （尚無景點安排）')
      } else {
        plan.attractions.forEach((a, idx) => {
          lines.push(`  ${idx + 1}. ${a.name}　${a.startTime}–${a.endTime}`)
          if (a.address) lines.push(`      📍 ${a.address}`)
        })
      }
    }

    setSummaryText(lines.join('\n'))
  }

  const handleCopyLink = async () => {
    if (!linkResult) return
    await navigator.clipboard.writeText(linkResult.url)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleCopySummary = async () => {
    if (!summaryText) return
    await navigator.clipboard.writeText(summaryText)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const formatExpiry = (iso: string) => {
    const d = new Date(iso)
    return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
  }

  return (
    <div className="space-y-5">
      {/* 分享方式選擇 */}
      <div className="bg-white rounded-2xl border border-gray-200 p-5">
        <h3 className="font-semibold text-gray-800 mb-4">選擇分享方式</h3>
        <div className="grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={() => handleSelect('LINK')}
            className={`text-left p-4 rounded-xl border transition-colors
              ${selected === 'LINK'
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-200 hover:border-gray-300 bg-white'}`}
          >
            <span className="text-2xl block mb-2">🔗</span>
            <p className={`text-sm font-medium ${selected === 'LINK' ? 'text-blue-700' : 'text-gray-700'}`}>
              連結分享
            </p>
            <p className="text-xs text-gray-400 mt-1">產生可分享的行程連結，有效期 7 天</p>
          </button>

          <button
            type="button"
            onClick={() => handleSelect('SUMMARY')}
            className={`text-left p-4 rounded-xl border transition-colors
              ${selected === 'SUMMARY'
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-200 hover:border-gray-300 bg-white'}`}
          >
            <span className="text-2xl block mb-2">📄</span>
            <p className={`text-sm font-medium ${selected === 'SUMMARY' ? 'text-blue-700' : 'text-gray-700'}`}>
              文字摘要
            </p>
            <p className="text-xs text-gray-400 mt-1">將行程排列成文字，方便貼到通訊軟體</p>
          </button>
        </div>

        {selected === 'LINK' && (
          <Button
            className="mt-4 w-full justify-center"
            onClick={handleGenerateLink}
            disabled={sharing}
          >
            {sharing ? '產生中…' : '產生分享連結'}
          </Button>
        )}

        {selected === 'SUMMARY' && (
          <Button
            className="mt-4 w-full justify-center"
            onClick={handleGenerateSummary}
          >
            產生文字摘要
          </Button>
        )}

        {error && <p className="text-sm text-red-500 mt-3">{error}</p>}
      </div>

      {/* 連結分享結果 */}
      {selected === 'LINK' && linkResult && (
        <div className="bg-white rounded-2xl border border-green-200 p-5">
          <div className="flex items-center gap-2 mb-4">
            <span className="text-green-500 text-xl">✓</span>
            <h3 className="font-semibold text-gray-800">分享連結已產生</h3>
          </div>

          <div className="bg-gray-50 rounded-xl border border-gray-200 p-4 mb-4">
            <p className="text-xs text-gray-400 mb-1">分享連結</p>
            <p className="text-sm text-gray-700 break-all font-mono leading-relaxed">
              {linkResult.url}
            </p>
          </div>

          <div className="flex items-center justify-between">
            <p className="text-xs text-gray-400">有效期限：{formatExpiry(linkResult.expiresAt)}</p>
            <Button variant={copied ? 'secondary' : 'primary'} onClick={handleCopyLink}>
              {copied ? '✓ 已複製' : '複製連結'}
            </Button>
          </div>

          <div className="mt-3 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
            <p className="text-xs text-amber-700">
              ⏱ 此連結將於 {formatExpiry(linkResult.expiresAt)} 後失效，之後需重新產生。
            </p>
          </div>
        </div>
      )}

      {/* 文字摘要結果 */}
      {selected === 'SUMMARY' && summaryText && (
        <div className="bg-white rounded-2xl border border-green-200 p-5">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <span className="text-green-500 text-xl">✓</span>
              <h3 className="font-semibold text-gray-800">行程文字摘要</h3>
            </div>
            <Button variant={copied ? 'secondary' : 'primary'} onClick={handleCopySummary}>
              {copied ? '✓ 已複製' : '複製文字'}
            </Button>
          </div>

          <textarea
            readOnly
            value={summaryText}
            rows={Math.min(summaryText.split('\n').length + 2, 20)}
            className="w-full text-sm text-gray-700 bg-gray-50 border border-gray-200 rounded-xl p-4 font-mono leading-relaxed resize-none focus:outline-none"
            onClick={e => (e.target as HTMLTextAreaElement).select()}
          />
        </div>
      )}

      {/* 說明 */}
      <div className="bg-gray-50 rounded-2xl border border-gray-200 p-5">
        <h3 className="font-semibold text-gray-700 mb-3 text-sm">關於行程分享</h3>
        <ul className="text-xs text-gray-500 space-y-2">
          <li className="flex items-start gap-2">
            <span className="mt-0.5">🔗</span>
            <span>連結分享：對方點擊連結即可瀏覽完整每日行程（有效期 7 天）</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5">📄</span>
            <span>文字摘要：將行程格式化為文字，可直接貼至 LINE、Messenger 等通訊軟體</span>
          </li>
        </ul>
      </div>
    </div>
  )
}
