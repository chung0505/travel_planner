package com.travel.planner.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false)
    private LocalDate returnDate;

    @Column(nullable = false)
    private int companionCount;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC")
    private List<DailyPlan> dailyPlans = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Route> routes = new ArrayList<>();

    protected Trip() {}

    public Trip(String title, String destination, LocalDate departureDate, LocalDate returnDate, int companionCount) {
        this.title = title;
        this.destination = destination;
        this.departureDate = departureDate;
        this.returnDate = returnDate;
        this.companionCount = companionCount;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDestination() { return destination; }
    public LocalDate getDepartureDate() { return departureDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public int getCompanionCount() { return companionCount; }
    public List<DailyPlan> getDailyPlans() { return dailyPlans; }
    public List<Route> getRoutes() { return routes; }

    public void setTitle(String title) { this.title = title; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public void setCompanionCount(int companionCount) { this.companionCount = companionCount; }
}
