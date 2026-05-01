package com.travel.planner.model;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "attractions")
public class Attraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_plan_id", nullable = false)
    private DailyPlan dailyPlan;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private Double latitude;

    private Double longitude;

    protected Attraction() {}

    public Attraction(DailyPlan dailyPlan, String name, String address, LocalTime startTime, LocalTime endTime) {
        this.dailyPlan = dailyPlan;
        this.name = name;
        this.address = address;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() { return id; }
    public DailyPlan getDailyPlan() { return dailyPlan; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }

    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }

    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
