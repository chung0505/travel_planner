package com.travel.planner.repository;

import com.travel.planner.model.DailyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {

    List<DailyPlan> findByTripIdOrderByDateAsc(Long tripId);

    Optional<DailyPlan> findByIdAndTripId(Long id, Long tripId);
}
