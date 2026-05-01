package com.travel.planner.service;

import java.util.List;

public record OrsRouteResult(
        double distanceMeters,
        double durationSeconds,
        List<double[]> geometry  // [lat, lng] pairs (Leaflet format)
) {}
