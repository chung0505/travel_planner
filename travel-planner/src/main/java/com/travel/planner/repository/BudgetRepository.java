package com.travel.planner.repository;

import com.travel.planner.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByTripId(Long tripId);
    boolean existsByTripId(Long tripId);
}
