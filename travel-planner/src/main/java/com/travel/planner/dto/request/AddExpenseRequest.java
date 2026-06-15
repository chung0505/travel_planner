package com.travel.planner.dto.request;

import com.travel.planner.model.enums.ExpenseType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AddExpenseRequest {

    @NotNull(message = "費用類型為必填欄位")
    private ExpenseType expenseType;

    @NotNull(message = "金額為必填欄位")
    @DecimalMin(value = "0.01", message = "金額必須大於 0")
    private BigDecimal amount;

    @NotNull(message = "日期為必填欄位")
    private LocalDate date;

    private String note;

    @NotBlank(message = "幣別為必填欄位")
    private String currency = "TWD";

    @NotNull(message = "匯率為必填欄位")
    @DecimalMin(value = "0.0001", message = "匯率必須大於 0")
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @NotNull(message = "原始金額為必填欄位")
    @DecimalMin(value = "0.01", message = "原始金額必須大於 0")
    private BigDecimal originalAmount;

    private String paidBy;

    private Long paidByTravelerId;

    private List<Long> splitAmongTravelerIds = new ArrayList<>();

    // key = travelerId, value = 比例數字（例如 {1: 2, 2: 1} 表示 2:1）
    // 若為 null 或空，則平均分攤
    private java.util.Map<Long, BigDecimal> splitRatios;

    public ExpenseType getExpenseType() { return expenseType; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public String getCurrency() { return currency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public String getPaidBy() { return paidBy; }
    public Long getPaidByTravelerId() { return paidByTravelerId; }
    public List<Long> getSplitAmongTravelerIds() { return splitAmongTravelerIds; }
    public java.util.Map<Long, BigDecimal> getSplitRatios() { return splitRatios; }

    public void setExpenseType(ExpenseType expenseType) { this.expenseType = expenseType; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }
    public void setPaidByTravelerId(Long paidByTravelerId) { this.paidByTravelerId = paidByTravelerId; }
    public void setSplitAmongTravelerIds(List<Long> splitAmongTravelerIds) { this.splitAmongTravelerIds = splitAmongTravelerIds; }
    public void setSplitRatios(java.util.Map<Long, BigDecimal> splitRatios) { this.splitRatios = splitRatios; }
}
