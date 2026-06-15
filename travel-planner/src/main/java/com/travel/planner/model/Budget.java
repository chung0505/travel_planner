package com.travel.planner.model;

import com.travel.planner.exception.InvalidInputException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBudget;

    @Column(nullable = false)
    private String currency;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC")
    private List<Expense> expenses = new ArrayList<>();

    protected Budget() {}

    public Budget(Trip trip, BigDecimal totalBudget, String currency) {
        if (totalBudget == null || totalBudget.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidInputException("預算金額不得為負數");
        }
        this.trip = trip;
        this.totalBudget = totalBudget;
        this.currency = currency;
    }

    public Long getId() { return id; }
    public Trip getTrip() { return trip; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public String getCurrency() { return currency; }
    public List<Expense> getExpenses() { return expenses; }

    public void setTotalBudget(BigDecimal totalBudget) {
        if (totalBudget == null || totalBudget.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidInputException("預算金額不得為負數");
        }
        this.totalBudget = totalBudget;
    }

    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal calculateTotalSpent() {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateRemainingBudget() {
        return totalBudget.subtract(calculateTotalSpent());
    }

    public boolean isOverBudget() {
        return calculateTotalSpent().compareTo(totalBudget) > 0;
    }
}
