package com.travel.planner.dto.response;

import com.travel.planner.model.enums.TransportationMethod;

import java.math.BigDecimal;
import java.util.List;

public class RouteEstimateResponse {
    private final List<Long> attractionIds;
    private final TransportationMethod transportationMethod;
    private final List<RouteSegmentResponse> segments;
    private final int totalEstimatedMinutes;
    private final BigDecimal totalEstimatedCost;
    private final List<double[]> geometry;

    public RouteEstimateResponse(List<Long> attractionIds, TransportationMethod transportationMethod,
                                  List<RouteSegmentResponse> segments, int totalEstimatedMinutes,
                                  BigDecimal totalEstimatedCost, List<double[]> geometry) {
        this.attractionIds = attractionIds;
        this.transportationMethod = transportationMethod;
        this.segments = segments;
        this.totalEstimatedMinutes = totalEstimatedMinutes;
        this.totalEstimatedCost = totalEstimatedCost;
        this.geometry = geometry;
    }

    public List<Long> getAttractionIds() { return attractionIds; }
    public TransportationMethod getTransportationMethod() { return transportationMethod; }
    public List<RouteSegmentResponse> getSegments() { return segments; }
    public int getTotalEstimatedMinutes() { return totalEstimatedMinutes; }
    public BigDecimal getTotalEstimatedCost() { return totalEstimatedCost; }
    public List<double[]> getGeometry() { return geometry; }
}
