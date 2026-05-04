package com.travel.planner.service;

import com.travel.planner.dto.response.TransitStepInfo;

import java.util.List;

public record OrsRouteResult(
        double distanceMeters,
        double durationSeconds,
        List<double[]> geometry,                      // [lat, lng] pairs
        List<double[]> legData,                       // [[distMeters, durSeconds]] per segment
        List<Double> legFares,                        // 每段實際費用（transit 由 API 提供，其餘為 null）
        List<List<TransitStepInfo>> segmentSteps      // 每段的交通步驟（transit 才有，其餘為 null）
) {}
