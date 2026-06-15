import { useState, useEffect } from 'react'
import Button from '../common/Button'
import SetBudgetModal from './SetBudgetModal'
import AddExpenseModal from './AddExpenseModal'
import { budgetApi } from '../../api/budget'
import type { BudgetSummaryResponse, SettlementResponse, TravelerResponse } from '../../types'
import { EXPENSE_TYPE_LABELS, EXPENSE_TYPE_ICONS } from '../../types'

interface Props {
  tripId: number
  participants: TravelerResponse[]
}

export default function BudgetPanel({ tripId, participants }: Props) {
  const [budget, setBudget] = useState<BudgetSummaryResponse | null>(null)
  const [settlement, setSettlement] = useState<SettlementResponse | null>(null)
  const [loadingSettlement, setLoadingSettlement] = useState(false)
  const [loading, setLoading] = useState(true)
  const [showSetBudget, setShowSetBudget] = useState(false)
  const [showAddExpense, setShowAddExpense] = useState(false)
  const [showSettlement, setShowSettlement] = useState(false)

  useEffect(() => {
    budgetApi.get(tripId)
      .then(setBudget)
      .catch(() => setBudget(null))
      .finally(() => setLoading(false))
  }, [tripId])

  const handleShowSettlement = async () => {
    setShowSettlement(true)
    if (settlement) return
    setLoadingSettlement(true)
    try {
      const s = await budgetApi.getSettlement(tripId)
      setSettlement(s)
    } catch {
      // 沒有分攤資料時靜默
    } finally {
      setLoadingSettlement(false)
    }
  }

  const handleExpenseAdded = async (b: BudgetSummaryResponse) => {
    setBudget(b)
    setShowAddExpense(false)
    if (showSettlement) {
      setLoadingSettlement(true)
      try {
        const s = await budgetApi.getSettlement(tripId)
        setSettlement(s)
      } catch {
        setSettlement(null)
      } finally {
        setLoadingSettlement(false)
      }
    } else {
      setSettlement(null)
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <span className="w-7 h-7 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
      </div>
    )
  }

  if (!budget) {
    return (
      <div className="text-center py-16">
        <p className="text-4xl mb-3">💰</p>
        <p className="text-gray-500 font-medium">尚未設定旅遊預算</p>
        <p className="text-gray-400 text-sm mt-1">設定預算後即可記錄各項花費</p>
        <Button className="mt-5" onClick={() => setShowSetBudget(true)}>設定預算</Button>
        {showSetBudget && (
          <SetBudgetModal
            tripId={tripId}
            onClose={() => setShowSetBudget(false)}
            onSaved={b => { setBudget(b); setShowSetBudget(false) }}
          />
        )}
      </div>
    )
  }

  const spentPercent = budget.totalBudget > 0
    ? Math.min((budget.totalSpent / budget.totalBudget) * 100, 100)
    : 0

  const hasSharings = budget.expenses.some(e => e.sharings.length > 0)

  return (
    <div className="space-y-5">
      {/* 預算摘要卡片 */}
      <div className={`rounded-2xl border p-5 ${budget.overBudget ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white'}`}>
        <div className="flex items-start justify-between mb-4">
          <div>
            <p className="text-sm text-gray-500">總預算</p>
            <p className="text-2xl font-bold text-gray-800">
              {budget.totalBudget.toLocaleString()}
              <span className="text-sm font-normal text-gray-500 ml-1">{budget.currency}</span>
            </p>
          </div>
          <Button variant="secondary" onClick={() => setShowSetBudget(true)}>修改預算</Button>
        </div>

        <div className="mb-3">
          <div className="h-2.5 bg-gray-200 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all ${budget.overBudget ? 'bg-red-500' : 'bg-blue-500'}`}
              style={{ width: `${spentPercent}%` }}
            />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <p className="text-gray-500">已花費</p>
            <p className={`font-semibold ${budget.overBudget ? 'text-red-600' : 'text-gray-800'}`}>
              {budget.totalSpent.toLocaleString()} {budget.currency}
            </p>
          </div>
          <div className="text-right">
            <p className="text-gray-500">剩餘預算</p>
            <p className={`font-semibold ${budget.overBudget ? 'text-red-600' : 'text-green-600'}`}>
              {budget.remainingBudget.toLocaleString()} {budget.currency}
            </p>
          </div>
        </div>

        {budget.overBudget && (
          <div className="mt-3 bg-red-100 text-red-700 text-sm rounded-lg px-3 py-2 flex items-center gap-2">
            ⚠️ 總花費已超出預算 {Math.abs(budget.remainingBudget).toLocaleString()} {budget.currency}
          </div>
        )}
      </div>

      {/* 費用清單 */}
      <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-800">費用明細</h3>
          <Button onClick={() => setShowAddExpense(true)}>+ 新增費用</Button>
        </div>

        {budget.expenses.length === 0 ? (
          <div className="text-center py-10 text-gray-400">
            <p className="text-3xl mb-2">📋</p>
            <p className="text-sm">尚未記錄任何費用</p>
          </div>
        ) : (
          <ul className="divide-y divide-gray-100">
            {budget.expenses.map(expense => (
              <li key={expense.id} className="px-5 py-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3">
                    <span className="text-xl mt-0.5">{EXPENSE_TYPE_ICONS[expense.expenseType]}</span>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium text-gray-800">
                          {EXPENSE_TYPE_LABELS[expense.expenseType]}
                        </span>
                        {expense.paidBy && (
                          <span className="text-xs bg-gray-100 text-gray-500 px-1.5 py-0.5 rounded">
                            {expense.paidBy} 付款
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-gray-400 mt-0.5">{expense.date}</p>
                      {expense.note && (
                        <p className="text-xs text-gray-500 mt-0.5">{expense.note}</p>
                      )}
                      {expense.currency !== budget.currency && (
                        <p className="text-xs text-gray-400 mt-0.5">
                          原始：{expense.originalAmount.toLocaleString()} {expense.currency}
                          （匯率 {expense.exchangeRate}）
                        </p>
                      )}
                      {expense.sharings.length > 0 && (
                        <div className="mt-1.5 flex flex-wrap gap-1">
                          {expense.sharings.map(s => (
                            <span key={s.id} className="text-xs bg-blue-50 text-blue-600 px-2 py-0.5 rounded-full">
                              {s.travelerName} {s.amountPerPerson.toLocaleString()} {budget.currency}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                  <p className="text-sm font-semibold text-gray-800 shrink-0">
                    {expense.amount.toLocaleString()} {budget.currency}
                  </p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* 類別統計 */}
      {budget.expenses.length > 0 && (
        <div className="bg-white rounded-2xl border border-gray-200 p-5">
          <h3 className="font-semibold text-gray-800 mb-3">類別統計</h3>
          <div className="space-y-2">
            {Object.entries(
              budget.expenses.reduce((acc, e) => {
                acc[e.expenseType] = (acc[e.expenseType] ?? 0) + e.amount
                return acc
              }, {} as Record<string, number>)
            )
              .sort(([, a], [, b]) => b - a)
              .map(([type, total]) => (
                <div key={type} className="flex items-center gap-3">
                  <span className="w-5 text-center">{EXPENSE_TYPE_ICONS[type as keyof typeof EXPENSE_TYPE_ICONS]}</span>
                  <span className="text-sm text-gray-600 flex-1">{EXPENSE_TYPE_LABELS[type as keyof typeof EXPENSE_TYPE_LABELS]}</span>
                  <div className="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-blue-400 rounded-full"
                      style={{ width: `${(total / budget.totalSpent) * 100}%` }}
                    />
                  </div>
                  <span className="text-sm font-medium text-gray-700 w-28 text-right">
                    {total.toLocaleString()} {budget.currency}
                  </span>
                </div>
              ))}
          </div>
        </div>
      )}

      {/* 費用結算 */}
      {hasSharings && (
        <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden">
          <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
            <h3 className="font-semibold text-gray-800">費用結算</h3>
            {!showSettlement && (
              <Button variant="secondary" onClick={handleShowSettlement}>查看結算</Button>
            )}
          </div>

          {showSettlement && (
            <div className="p-5 space-y-5">
              {loadingSettlement ? (
                <div className="flex justify-center py-6">
                  <span className="w-6 h-6 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
                </div>
              ) : settlement ? (
                <>
                  {/* 每人收支狀況 */}
                  <div>
                    <p className="text-sm font-medium text-gray-600 mb-2">每人收支</p>
                    <div className="space-y-2">
                      {settlement.balances.map(b => (
                        <div key={b.travelerId} className="flex items-center justify-between text-sm">
                          <span className="text-gray-700">{b.travelerName}</span>
                          <div className="flex gap-4 text-right">
                            <span className="text-gray-400 text-xs">
                              付出 {b.totalPaid.toLocaleString()} / 應付 {b.totalOwed.toLocaleString()}
                            </span>
                            <span className={`font-semibold w-28 ${b.balance > 0 ? 'text-green-600' : b.balance < 0 ? 'text-red-600' : 'text-gray-400'}`}>
                              {b.balance > 0 ? `+${b.balance.toLocaleString()}` : b.balance.toLocaleString()} {budget.currency}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                    <p className="text-xs text-gray-400 mt-2">正數＝別人欠你，負數＝你欠別人</p>
                  </div>

                  {/* 轉帳清單 */}
                  {settlement.transfers.length > 0 ? (
                    <div>
                      <p className="text-sm font-medium text-gray-600 mb-2">結清方式</p>
                      <div className="space-y-2">
                        {settlement.transfers.map((t, i) => (
                          <div key={i} className="flex items-center gap-2 text-sm bg-amber-50 rounded-lg px-3 py-2">
                            <span className="text-gray-700">{t.fromTravelerName}</span>
                            <span className="text-gray-400">→</span>
                            <span className="text-gray-700">{t.toTravelerName}</span>
                            <span className="ml-auto font-semibold text-amber-700">
                              {t.amount.toLocaleString()} {budget.currency}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <p className="text-sm text-gray-400 text-center py-2">已結清，無需轉帳</p>
                  )}
                </>
              ) : (
                <p className="text-sm text-gray-400 text-center py-4">無法取得結算資料</p>
              )}
            </div>
          )}
        </div>
      )}

      {showSetBudget && (
        <SetBudgetModal
          tripId={tripId}
          current={budget}
          onClose={() => setShowSetBudget(false)}
          onSaved={b => { setBudget(b); setShowSetBudget(false) }}
        />
      )}
      {showAddExpense && (
        <AddExpenseModal
          tripId={tripId}
          defaultCurrency={budget.currency}
          travelers={participants}
          onClose={() => setShowAddExpense(false)}
          onAdded={handleExpenseAdded}
        />
      )}
    </div>
  )
}
