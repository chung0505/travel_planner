package com.travel.planner.repository;

import com.travel.planner.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByDailyPlanId(Long dailyPlanId);

    Optional<Route> findByIdAndDailyPlanId(Long id, Long dailyPlanId);
}
