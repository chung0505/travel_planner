import { useState } from 'react'
import Modal from '../common/Modal'
import Button from '../common/Button'
import FormField from '../common/FormField'
import { budgetApi } from '../../api/budget'
import type { BudgetSummaryResponse } from '../../types'

const CURRENCIES = ['TWD', 'JPY', 'USD', 'EUR', 'KRW', 'HKD', 'CNY']

interface Props {
  tripId: number
  current?: BudgetSummaryResponse
  onClose: () => void
  onSaved: (budget: BudgetSummaryResponse) => void
}

export default function SetBudgetModal({ tripId, current, onClose, onSaved }: Props) {
  const [totalBudget, setTotalBudget] = useState(current?.totalBudget?.toString() ?? '')
  const [currency, setCurrency] = useState(current?.currency ?? 'TWD')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const amount = parseFloat(totalBudget)
    if (isNaN(amount) || amount < 0) {
      setError('請輸入有效的預算金額')
      return
    }
    setSaving(true)
    try {
      const result = await budgetApi.set(tripId, { totalBudget: amount, currency })
      onSaved(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : '設定預算失敗')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={current ? '修改預算' : '設定預算'} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <FormField
          label="總預算金額"
          type="number"
          min="0"
          step="1"
          value={totalBudget}
          onChange={e => setTotalBudget(e.target.value)}
          required
          placeholder="例：50000"
        />
        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-gray-700">
            幣別 <span className="text-red-500">*</span>
          </label>
          <select
            value={currency}
            onChange={e => setCurrency(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          >
            {CURRENCIES.map(c => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
        {error && <p className="text-sm text-red-500">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="ghost" onClick={onClose}>取消</Button>
          <Button type="submit" disabled={saving}>
            {saving ? '儲存中…' : '確認儲存'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
