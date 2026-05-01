package com.travel.planner.dto.response;

import java.math.BigDecimal;

public class RouteSegmentResponse {
    private final String fromAttraction;
    private final String toAttraction;
    private final int estimatedMinutes;
    private final BigDecimal estimatedCost;

    public RouteSegmentResponse(String fromAttraction, String toAttraction,
                                 int estimatedMinutes, BigDecimal estimatedCost) {
        this.fromAttraction = fromAttraction;
        this.toAttraction = toAttraction;
        this.estimatedMinutes = estimatedMinutes;
        this.estimatedCost = estimatedCost;
    }

    public String getFromAttraction() { return fromAttraction; }
    public String getToAttraction() { return toAttraction; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
}
