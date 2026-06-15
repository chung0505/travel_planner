package com.travel.planner.repository;

import com.travel.planner.model.ExpenseSharing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseSharingRepository extends JpaRepository<ExpenseSharing, Long> {
    List<ExpenseSharing> findByExpenseId(Long expenseId);
}
