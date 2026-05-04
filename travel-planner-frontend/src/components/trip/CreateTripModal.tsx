import { useState } from 'react'
import Modal from '../common/Modal'
import Button from '../common/Button'
import FormField from '../common/FormField'
import { tripsApi } from '../../api/trips'
import type { TripResponse } from '../../types'

interface Props {
  onClose: () => void
  onCreated: (trip: TripResponse) => void
}

export default function CreateTripModal({ onClose, onCreated }: Props) {
  const [form, setForm] = useState({
    title: '',
    destination: '',
    departureDate: '',
    returnDate: '',
    companionCount: 1,
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const set = (field: string, value: string | number) =>
    setForm(f => ({ ...f, [field]: value }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!form.title || !form.destination || !form.departureDate || !form.returnDate) {
      setError('請填寫所有必填欄位')
      return
    }
    if (form.returnDate <= form.departureDate) {
      setError('回程日期必須晚於出發日期')
      return
    }

    setLoading(true)
    try {
      const trip = await tripsApi.create({
        ...form,
        companionCount: Number(form.companionCount),
      })
      onCreated(trip)
    } catch (err) {
      setError(err instanceof Error ? err.message : '建立失敗，請稍後再試')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal title="建立新行程" onClose={onClose}>
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <FormField
          label="行程名稱"
          required
          placeholder="例：東京五天四夜、首爾春季之旅"
          value={form.title}
          onChange={e => set('title', e.target.value)}
        />
        <FormField
          label="目的地"
          required
          placeholder="例：東京、首爾、巴黎"
          value={form.destination}
          onChange={e => set('destination', e.target.value)}
        />
        <div className="grid grid-cols-2 gap-3">
          <FormField
            label="出發日期"
            type="date"
            required
            value={form.departureDate}
            onChange={e => set('departureDate', e.target.value)}
          />
          <FormField
            label="回程日期"
            type="date"
            required
            value={form.returnDate}
            min={form.departureDate}
            onChange={e => set('returnDate', e.target.value)}
          />
        </div>
        <FormField
          label="旅伴人數"
          type="number"
          min={1}
          required
          value={form.companionCount}
          onChange={e => set('companionCount', e.target.value)}
        />

        {error && (
          <p className="text-sm text-red-500 bg-red-50 rounded-lg px-3 py-2">{error}</p>
        )}

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" type="button" onClick={onClose}>
            取消
          </Button>
          <Button type="submit" loading={loading}>
            建立行程
          </Button>
        </div>
      </form>
    </Modal>
  )
}
