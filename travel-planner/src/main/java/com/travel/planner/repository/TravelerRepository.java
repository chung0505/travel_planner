package com.travel.planner.repository;

import com.travel.planner.model.Traveler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelerRepository extends JpaRepository<Traveler, Long> {
    Optional<Traveler> findByEmail(String email);
    boolean existsByEmail(String email);
}
