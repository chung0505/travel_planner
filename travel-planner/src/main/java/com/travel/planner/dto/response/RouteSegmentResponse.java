package com.travel.planner.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class RouteSegmentResponse {
    private final String fromAttraction;
    private final String toAttraction;
    private final int estimatedMinutes;
    private final BigDecimal estimatedCost;
    private final List<TransitStepInfo> transitSteps; // transit 才有，其餘為 null

    public RouteSegmentResponse(String fromAttraction, String toAttraction,
                                int estimatedMinutes, BigDecimal estimatedCost,
                                List<TransitStepInfo> transitSteps) {
        this.fromAttraction = fromAttraction;
        this.toAttraction   = toAttraction;
        this.estimatedMinutes = estimatedMinutes;
        this.estimatedCost  = estimatedCost;
        this.transitSteps   = transitSteps;
    }

    public String getFromAttraction()          { return fromAttraction; }
    public String getToAttraction()            { return toAttraction; }
    public int getEstimatedMinutes()           { return estimatedMinutes; }
    public BigDecimal getEstimatedCost()       { return estimatedCost; }
    public List<TransitStepInfo> getTransitSteps() { return transitSteps; }
}
