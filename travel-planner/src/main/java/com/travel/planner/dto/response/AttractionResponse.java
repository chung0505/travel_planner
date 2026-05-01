package com.travel.planner.dto.response;

import com.travel.planner.model.Attraction;

import java.time.LocalTime;

public class AttractionResponse {

    private final Long id;
    private final String name;
    private final String address;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Double latitude;
    private final Double longitude;

    public AttractionResponse(Attraction attraction) {
        this.id = attraction.getId();
        this.name = attraction.getName();
        this.address = attraction.getAddress();
        this.startTime = attraction.getStartTime();
        this.endTime = attraction.getEndTime();
        this.latitude = attraction.getLatitude();
        this.longitude = attraction.getLongitude();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
