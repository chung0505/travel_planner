package com.travel.planner.dto.response;

import com.travel.planner.model.Trip;

import java.time.LocalDate;
import java.util.List;

public class TripResponse {

    private final Long id;
    private final String destination;
    private final LocalDate departureDate;
    private final LocalDate returnDate;
    private final int companionCount;
    private final int totalDays;
    private final List<DailyPlanResponse> dailyPlans;

    public TripResponse(Trip trip) {
        this.id = trip.getId();
        this.destination = trip.getDestination();
        this.departureDate = trip.getDepartureDate();
        this.returnDate = trip.getReturnDate();
        this.companionCount = trip.getCompanionCount();
        this.totalDays = trip.getDailyPlans().size();
        this.dailyPlans = trip.getDailyPlans().stream()
                .map(DailyPlanResponse::new)
                .toList();
    }

    public Long getId() { return id; }
    public String getDestination() { return destination; }
    public LocalDate getDepartureDate() { return departureDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public int getCompanionCount() { return companionCount; }
    public int getTotalDays() { return totalDays; }
    public List<DailyPlanResponse> getDailyPlans() { return dailyPlans; }
}
