package com.travel.planner.repository;

import com.travel.planner.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByBudgetIdOrderByDateAsc(Long budgetId);
}
