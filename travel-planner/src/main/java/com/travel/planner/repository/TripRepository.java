package com.travel.planner.repository;

import com.travel.planner.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    java.util.List<Trip> findByOrganizerId(Long organizerId);
    java.util.List<Trip> findByParticipantsId(Long travelerId);
}
