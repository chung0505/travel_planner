package com.travel.planner.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SetBudgetRequest {

    @NotNull(message = "預算金額為必填欄位")
    @DecimalMin(value = "0.0", message = "預算金額不得為負數")
    private BigDecimal totalBudget;

    @NotBlank(message = "幣別為必填欄位")
    private String currency = "TWD";

    public BigDecimal getTotalBudget() { return totalBudget; }
    public String getCurrency() { return currency; }

    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }
    public void setCurrency(String currency) { this.currency = currency; }
}
