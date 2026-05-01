package com.travel.planner.dto.response;

import com.travel.planner.model.Route;
import com.travel.planner.model.enums.TransportationMethod;

import java.math.BigDecimal;
import java.util.List;

public class RouteResponse {

    private final Long id;
    private final Long tripId;
    private final List<Long> attractionIds;
    private final TransportationMethod transportationMethod;
    private final int estimatedDurationMinutes;
    private final BigDecimal estimatedCost;
    private final boolean confirmed;
    private final List<double[]> geometry;

    public RouteResponse(Route route, List<double[]> geometry) {
        this.id = route.getId();
        this.tripId = route.getTrip().getId();
        this.attractionIds = route.getAttractionIds();
        this.transportationMethod = route.getTransportationMethod();
        this.estimatedDurationMinutes = route.getEstimatedDurationMinutes();
        this.estimatedCost = route.getEstimatedCost();
        this.confirmed = route.isConfirmed();
        this.geometry = geometry;
    }

    public Long getId() { return id; }
    public Long getTripId() { return tripId; }
    public List<Long> getAttractionIds() { return attractionIds; }
    public TransportationMethod getTransportationMethod() { return transportationMethod; }
    public int getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public boolean isConfirmed() { return confirmed; }
    public List<double[]> getGeometry() { return geometry; }
}
