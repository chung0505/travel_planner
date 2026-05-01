import { useState } from 'react'
import Modal from '../common/Modal'
import Button from '../common/Button'
import FormField from '../common/FormField'
import { dailyPlansApi } from '../../api/dailyPlans'
import type { DailyPlanResponse } from '../../types'

interface Props {
  tripId: number
  dailyPlanId: number
  date: string
  onClose: () => void
  onAdded: (plan: DailyPlanResponse) => void
}

export default function AddAttractionModal({ tripId, dailyPlanId, date, onClose, onAdded }: Props) {
  const [form, setForm] = useState({ name: '', address: '', startTime: '', endTime: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const set = (field: string, value: string) => setForm(f => ({ ...f, [field]: value }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!form.name || !form.address || !form.startTime || !form.endTime) {
      setError('請填寫所有必填欄位')
      return
    }
    if (form.endTime <= form.startTime) {
      setError('結束時間必須晚於開始時間')
      return
    }

    setLoading(true)
    try {
      const updated = await dailyPlansApi.addAttraction(tripId, dailyPlanId, form)
      onAdded(updated)
    } catch (err) {
      setError(err instanceof Error ? err.message : '新增失敗，請稍後再試')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal title={`新增景點 — ${date}`} onClose={onClose}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <FormField
          label="景點名稱"
          required
          placeholder="例：淺草寺"
          value={form.name}
          onChange={e => set('name', e.target.value)}
        />
        <FormField
          label="地址"
          required
          placeholder="例：東京都台東區淺草2-3-1"
          value={form.address}
          onChange={e => set('address', e.target.value)}
        />
        <div className="grid grid-cols-2 gap-3">
          <FormField
            label="開始時間"
            type="time"
            required
            value={form.startTime}
            onChange={e => set('startTime', e.target.value)}
          />
          <FormField
            label="結束時間"
            type="time"
            required
            value={form.endTime}
            min={form.startTime}
            onChange={e => set('endTime', e.target.value)}
          />
        </div>

        {error && (
          <p className="text-sm text-red-500 bg-red-50 rounded-lg px-3 py-2">⚠️ {error}</p>
        )}

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>取消</Button>
          <Button type="submit" loading={loading}>新增景點</Button>
        </div>
      </form>
    </Modal>
  )
}
