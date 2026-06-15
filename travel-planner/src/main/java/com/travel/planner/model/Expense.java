package com.travel.planner.model;

import com.travel.planner.model.enums.ExpenseType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseType expenseType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    @Column
    private String note;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column
    private String paidBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_traveler_id")
    private Traveler paidByTraveler;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseSharing> sharings = new ArrayList<>();

    protected Expense() {}

    public Expense(Budget budget, ExpenseType expenseType, BigDecimal amount,
                   LocalDate date, String note, String currency,
                   BigDecimal exchangeRate, BigDecimal originalAmount,
                   String paidBy, Traveler paidByTraveler) {
        this.budget = budget;
        this.expenseType = expenseType;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.originalAmount = originalAmount;
        this.paidBy = paidBy;
        this.paidByTraveler = paidByTraveler;
    }

    public Long getId() { return id; }
    public Budget getBudget() { return budget; }
    public ExpenseType getExpenseType() { return expenseType; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public String getCurrency() { return currency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public String getPaidBy() { return paidBy; }
    public Traveler getPaidByTraveler() { return paidByTraveler; }
    public List<ExpenseSharing> getSharings() { return sharings; }
}
