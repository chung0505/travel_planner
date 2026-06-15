import { useState } from 'react'
import Modal from '../common/Modal'
import Button from '../common/Button'
import FormField from '../common/FormField'
import { budgetApi } from '../../api/budget'
import type { BudgetSummaryResponse, ExpenseType, TravelerResponse } from '../../types'
import { EXPENSE_TYPE_LABELS, EXPENSE_TYPE_ICONS } from '../../types'

const CURRENCIES = ['TWD', 'JPY', 'USD', 'EUR', 'KRW', 'HKD', 'CNY']
const EXPENSE_TYPES: ExpenseType[] = ['ACCOMMODATION', 'FOOD', 'TRANSPORTATION', 'TICKET', 'OTHER']

interface Props {
  tripId: number
  defaultCurrency: string
  travelers: TravelerResponse[]
  onClose: () => void
  onAdded: (budget: BudgetSummaryResponse) => void
}

export default function AddExpenseModal({ tripId, defaultCurrency, travelers, onClose, onAdded }: Props) {
  const today = new Date().toISOString().split('T')[0]

  const [expenseType, setExpenseType] = useState<ExpenseType>('OTHER')
  const [currency, setCurrency] = useState(defaultCurrency)
  const [originalAmount, setOriginalAmount] = useState('')
  const [exchangeRate, setExchangeRate] = useState('1')
  const [date, setDate] = useState(today)
  const [note, setNote] = useState('')
  const [paidByTravelerId, setPaidByTravelerId] = useState<number | ''>('')
  // 分攤旅客 ID 清單
  const [splitIds, setSplitIds] = useState<number[]>([])
  // 各旅客比例，key = travelerId
  const [ratios, setRatios] = useState<Record<number, string>>({})
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const computedAmount = () => {
    const orig = parseFloat(originalAmount)
    const rate = parseFloat(exchangeRate)
    if (isNaN(orig) || isNaN(rate) || rate <= 0) return NaN
    return parseFloat((orig * rate).toFixed(2))
  }

  const toggleSplit = (id: number) => {
    setSplitIds(prev => {
      const next = prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
      // 新加入的旅客預設比例 1
      if (!prev.includes(id)) {
        setRatios(r => ({ ...r, [id]: r[id] ?? '1' }))
      }
      return next
    })
  }

  // 依比例計算每人金額預覽
  const previewShares = () => {
    const amount = computedAmount()
    if (isNaN(amount) || splitIds.length === 0) return []
    const totalRatio = splitIds.reduce((sum, id) => sum + (parseFloat(ratios[id]) || 1), 0)
    if (totalRatio <= 0) return []
    return splitIds.map(id => {
      const ratio = parseFloat(ratios[id]) || 1
      return { id, name: travelers.find(t => t.id === id)?.name ?? id, share: (amount * ratio / totalRatio) }
    })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const orig = parseFloat(originalAmount)
    const rate = parseFloat(exchangeRate)
    const amount = computedAmount()
    if (isNaN(orig) || orig <= 0) { setError('請輸入有效的金額'); return }
    if (isNaN(rate) || rate <= 0) { setError('請輸入有效的匯率'); return }
    if (!date) { setError('請選擇日期'); return }

    const splitRatios: Record<number, number> | undefined = splitIds.length > 0
      ? Object.fromEntries(splitIds.map(id => [id, parseFloat(ratios[id]) || 1]))
      : undefined

    setSaving(true)
    try {
      const result = await budgetApi.addExpense(tripId, {
        expenseType,
        amount,
        date,
        note: note || undefined,
        currency,
        exchangeRate: rate,
        originalAmount: orig,
        paidByTravelerId: paidByTravelerId !== '' ? paidByTravelerId : undefined,
        splitAmongTravelerIds: splitIds.length > 0 ? splitIds : undefined,
        splitRatios,
      })
      onAdded(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : '新增費用失敗')
    } finally {
      setSaving(false)
    }
  }

  const amount = computedAmount()
  const shares = previewShares()

  return (
    <Modal title="新增費用" onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* 費用類型 */}
        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-gray-700">費用類型 <span className="text-red-500">*</span></label>
          <div className="grid grid-cols-5 gap-2">
            {EXPENSE_TYPES.map(t => (
              <button
                key={t}
                type="button"
                onClick={() => setExpenseType(t)}
                className={`flex flex-col items-center gap-1 p-2 rounded-xl border text-xs transition-colors
                  ${expenseType === t
                    ? 'border-blue-500 bg-blue-50 text-blue-700'
                    : 'border-gray-200 text-gray-500 hover:border-gray-300'}`}
              >
                <span className="text-lg">{EXPENSE_TYPE_ICONS[t]}</span>
                {EXPENSE_TYPE_LABELS[t]}
              </button>
            ))}
          </div>
        </div>

        {/* 幣別 + 原始金額 */}
        <div className="grid grid-cols-3 gap-3">
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700">幣別</label>
            <select
              value={currency}
              onChange={e => { setCurrency(e.target.value); if (e.target.value === defaultCurrency) setExchangeRate('1') }}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
            >
              {CURRENCIES.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div className="col-span-2">
            <FormField
              label="金額"
              type="number"
              min="0.01"
              step="0.01"
              value={originalAmount}
              onChange={e => setOriginalAmount(e.target.value)}
              required
              placeholder="例：3000"
            />
          </div>
        </div>

        {/* 匯率 + 換算後金額 */}
        {currency !== defaultCurrency && (
          <div className="grid grid-cols-2 gap-3">
            <FormField
              label={`匯率（${currency} → ${defaultCurrency}）`}
              type="number"
              min="0.0001"
              step="0.0001"
              value={exchangeRate}
              onChange={e => setExchangeRate(e.target.value)}
              required
            />
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">換算後（{defaultCurrency}）</label>
              <div className="border border-gray-200 rounded-lg px-3 py-2 text-sm bg-gray-50 text-gray-600">
                {isNaN(amount) ? '—' : amount.toLocaleString()}
              </div>
            </div>
          </div>
        )}

        <FormField
          label="日期"
          type="date"
          value={date}
          onChange={e => setDate(e.target.value)}
          required
        />

        {/* 付款人（從旅客清單選） */}
        {travelers.length > 0 && (
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-gray-700">付款人</label>
            <select
              value={paidByTravelerId}
              onChange={e => setPaidByTravelerId(e.target.value === '' ? '' : Number(e.target.value))}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
            >
              <option value="">— 未指定 —</option>
              {travelers.map(t => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </div>
        )}

        {/* 分攤設定 */}
        {travelers.length > 0 && (
          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium text-gray-700">分攤對象與比例</label>
            <div className="space-y-2">
              {travelers.map(t => (
                <div key={t.id} className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => toggleSplit(t.id)}
                    className={`w-5 h-5 rounded border flex items-center justify-center flex-shrink-0 transition-colors
                      ${splitIds.includes(t.id)
                        ? 'bg-blue-600 border-blue-600 text-white'
                        : 'border-gray-300 bg-white'}`}
                  >
                    {splitIds.includes(t.id) && <span className="text-xs leading-none">✓</span>}
                  </button>
                  <span className="text-sm text-gray-700 flex-1">{t.name}</span>
                  {splitIds.includes(t.id) && (
                    <div className="flex items-center gap-1">
                      <input
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={ratios[t.id] ?? '1'}
                        onChange={e => setRatios(r => ({ ...r, [t.id]: e.target.value }))}
                        className="w-16 border border-gray-300 rounded px-2 py-1 text-sm text-center focus:outline-none focus:ring-1 focus:ring-blue-400"
                      />
                      <span className="text-xs text-gray-400">比例</span>
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* 即時預覽每人金額 */}
            {shares.length > 0 && (
              <div className="mt-1 rounded-lg bg-blue-50 px-3 py-2 space-y-1">
                <p className="text-xs font-medium text-blue-700 mb-1">分攤金額預覽</p>
                {shares.map(s => (
                  <div key={s.id} className="flex justify-between text-xs text-blue-600">
                    <span>{s.name}</span>
                    <span>{s.share.toLocaleString(undefined, { maximumFractionDigits: 2 })} {defaultCurrency}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        <FormField
          label="備註"
          type="text"
          value={note}
          onChange={e => setNote(e.target.value)}
          placeholder="選填"
        />

        {error && <p className="text-sm text-red-500">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="ghost" onClick={onClose}>取消</Button>
          <Button type="submit" disabled={saving}>{saving ? '新增中…' : '新增費用'}</Button>
        </div>
      </form>
    </Modal>
  )
}
