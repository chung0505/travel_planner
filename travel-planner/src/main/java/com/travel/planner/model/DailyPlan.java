package com.travel.planner.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_plans")
public class DailyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int dayNumber;

    @OneToMany(mappedBy = "dailyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startTime ASC")
    private List<Attraction> attractions = new ArrayList<>();

    protected DailyPlan() {}

    public DailyPlan(Trip trip, LocalDate date, int dayNumber) {
        this.trip = trip;
        this.date = date;
        this.dayNumber = dayNumber;
    }

    public Long getId() { return id; }
    public Trip getTrip() { return trip; }
    public LocalDate getDate() { return date; }
    public int getDayNumber() { return dayNumber; }
    public List<Attraction> getAttractions() { return attractions; }
}
