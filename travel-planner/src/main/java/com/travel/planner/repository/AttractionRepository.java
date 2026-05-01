package com.travel.planner.repository;

import com.travel.planner.model.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    List<Attraction> findByDailyPlanIdOrderByStartTimeAsc(Long dailyPlanId);

    @Query("SELECT a FROM Attraction a WHERE a.dailyPlan.trip.id = :tripId ORDER BY a.dailyPlan.date ASC, a.startTime ASC")
    List<Attraction> findByTripIdOrderByDateAndTime(Long tripId);
}
