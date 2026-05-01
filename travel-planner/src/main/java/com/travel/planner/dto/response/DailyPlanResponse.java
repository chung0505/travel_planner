package com.travel.planner.dto.response;

import com.travel.planner.model.DailyPlan;

import java.time.LocalDate;
import java.util.List;

public class DailyPlanResponse {

    private final Long id;
    private final LocalDate date;
    private final int dayNumber;
    private final List<AttractionResponse> attractions;

    public DailyPlanResponse(DailyPlan dailyPlan) {
        this.id = dailyPlan.getId();
        this.date = dailyPlan.getDate();
        this.dayNumber = dailyPlan.getDayNumber();
        this.attractions = dailyPlan.getAttractions().stream()
                .map(AttractionResponse::new)
                .toList();
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public int getDayNumber() { return dayNumber; }
    public List<AttractionResponse> getAttractions() { return attractions; }
}
