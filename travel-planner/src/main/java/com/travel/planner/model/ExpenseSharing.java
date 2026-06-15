package com.travel.planner.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_sharings")
public class ExpenseSharing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traveler_id", nullable = false)
    private Traveler traveler;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPerPerson;

    protected ExpenseSharing() {}

    public ExpenseSharing(Expense expense, Traveler traveler, BigDecimal amountPerPerson) {
        this.expense = expense;
        this.traveler = traveler;
        this.amountPerPerson = amountPerPerson;
    }

    public Long getId() { return id; }
    public Expense getExpense() { return expense; }
    public Traveler getTraveler() { return traveler; }
    public BigDecimal getAmountPerPerson() { return amountPerPerson; }
}
