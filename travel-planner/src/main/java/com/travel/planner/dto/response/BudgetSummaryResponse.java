package com.travel.planner.dto.response;

import com.travel.planner.model.Budget;
import com.travel.planner.model.Expense;

import java.math.BigDecimal;
import java.util.List;

public class BudgetSummaryResponse {

    private final Long budgetId;
    private final Long tripId;
    private final BigDecimal totalBudget;
    private final String currency;
    private final BigDecimal totalSpent;
    private final BigDecimal remainingBudget;
    private final boolean overBudget;
    private final List<ExpenseResponse> expenses;

    // 不帶 travelerId：計算整趟旅程總花費（用於 setBudget 回傳）
    public BudgetSummaryResponse(Budget budget) {
        this(budget, null);
    }

    // 帶 travelerId：計算當前使用者的個人花費
    public BudgetSummaryResponse(Budget budget, Long travelerId) {
        this.budgetId = budget.getId();
        this.tripId = budget.getTrip().getId();
        this.totalBudget = budget.getTotalBudget();
        this.currency = budget.getCurrency();
        this.totalSpent = calculateMySpent(budget, travelerId);
        this.remainingBudget = budget.getTotalBudget().subtract(this.totalSpent);
        this.overBudget = this.totalSpent.compareTo(budget.getTotalBudget()) > 0;
        this.expenses = budget.getExpenses().stream()
                .map(ExpenseResponse::new)
                .toList();
    }

    private static BigDecimal calculateMySpent(Budget budget, Long travelerId) {
        if (travelerId == null) {
            return budget.calculateTotalSpent();
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : budget.getExpenses()) {
            if (expense.getSharings().isEmpty()) {
                // 沒有設定分攤對象，算整筆（通用花費）
                total = total.add(expense.getAmount());
            } else {
                // 有分攤：只加當前使用者的那份
                total = total.add(
                    expense.getSharings().stream()
                        .filter(s -> s.getTraveler().getId().equals(travelerId))
                        .map(s -> s.getAmountPerPerson())
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                );
            }
        }
        return total;
    }

    public Long getBudgetId() { return budgetId; }
    public Long getTripId() { return tripId; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public String getCurrency() { return currency; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public BigDecimal getRemainingBudget() { return remainingBudget; }
    public boolean isOverBudget() { return overBudget; }
    public List<ExpenseResponse> getExpenses() { return expenses; }
}
