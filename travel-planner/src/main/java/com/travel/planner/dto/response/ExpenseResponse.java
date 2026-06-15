package com.travel.planner.dto.response;

import com.travel.planner.model.Expense;
import com.travel.planner.model.enums.ExpenseType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ExpenseResponse {

    private final Long id;
    private final ExpenseType expenseType;
    private final BigDecimal amount;
    private final LocalDate date;
    private final String note;
    private final String currency;
    private final BigDecimal exchangeRate;
    private final BigDecimal originalAmount;
    private final String paidBy;
    private final Long paidByTravelerId;
    private final List<ExpenseSharingResponse> sharings;

    public ExpenseResponse(Expense expense) {
        this.id = expense.getId();
        this.expenseType = expense.getExpenseType();
        this.amount = expense.getAmount();
        this.date = expense.getDate();
        this.note = expense.getNote();
        this.currency = expense.getCurrency();
        this.exchangeRate = expense.getExchangeRate();
        this.originalAmount = expense.getOriginalAmount();
        this.paidBy = expense.getPaidBy();
        this.paidByTravelerId = expense.getPaidByTraveler() != null
                ? expense.getPaidByTraveler().getId() : null;
        this.sharings = expense.getSharings().stream()
                .map(ExpenseSharingResponse::new)
                .toList();
    }

    public Long getId() { return id; }
    public ExpenseType getExpenseType() { return expenseType; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public String getCurrency() { return currency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public String getPaidBy() { return paidBy; }
    public Long getPaidByTravelerId() { return paidByTravelerId; }
    public List<ExpenseSharingResponse> getSharings() { return sharings; }
}
