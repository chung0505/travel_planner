package com.travel.planner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.planner.dto.request.PlanRouteRequest;
import com.travel.planner.dto.response.RouteEstimateResponse;
import com.travel.planner.dto.response.RouteResponse;
import com.travel.planner.dto.response.RouteSegmentResponse;
import com.travel.planner.exception.InvalidInputException;
import com.travel.planner.exception.ResourceNotFoundException;
import com.travel.planner.model.Attraction;
import com.travel.planner.model.Route;
import com.travel.planner.model.Trip;
import com.travel.planner.model.enums.TransportationMethod;
import com.travel.planner.repository.AttractionRepository;
import com.travel.planner.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UC-02: Plan Route
 * 串接 ORS API 計算真實路線、時間與費用
 */
@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final AttractionRepository attractionRepository;
    private final TripService tripService;
    private final OrsService orsService;
    private final ObjectMapper objectMapper;

    public RouteService(RouteRepository routeRepository,
                        AttractionRepository attractionRepository,
                        TripService tripService,
                        OrsService orsService,
                        ObjectMapper objectMapper) {
        this.routeRepository = routeRepository;
        this.attractionRepository = attractionRepository;
        this.tripService = tripService;
        this.orsService = orsService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public RouteEstimateResponse estimateRoute(Long tripId, PlanRouteRequest request) {
        tripService.findTripById(tripId);
        List<Attraction> attractions = resolveAttractions(tripId, request.getAttractionIds());
        List<double[]> coordinates = extractCoordinates(attractions);
        TransportationMethod method = request.getTransportationMethod();

        OrsRouteResult orsResult = orsService.getDirections(coordinates, method);

        List<RouteSegmentResponse> segments = buildSegments(attractions, orsResult, method);

        int totalMinutes = (int) Math.round(orsResult.durationSeconds() / 60.0);
        BigDecimal totalCost = calculateCost(method, orsResult.distanceMeters());

        return new RouteEstimateResponse(
                request.getAttractionIds(),
                method,
                segments,
                totalMinutes,
                totalCost,
                orsResult.geometry()
        );
    }

    @Transactional
    public RouteResponse confirmRoute(Long tripId, PlanRouteRequest request) {
        Trip trip = tripService.findTripById(tripId);
        List<Attraction> attractions = resolveAttractions(tripId, request.getAttractionIds());
        List<double[]> coordinates = extractCoordinates(attractions);
        TransportationMethod method = request.getTransportationMethod();

        OrsRouteResult orsResult = orsService.getDirections(coordinates, method);

        int totalMinutes = (int) Math.round(orsResult.durationSeconds() / 60.0);
        BigDecimal totalCost = calculateCost(method, orsResult.distanceMeters());

        Route route = new Route(trip, request.getAttractionIds(), method, totalMinutes, totalCost);
        route.setConfirmed(true);
        route.setGeometryJson(serializeGeometry(orsResult.geometry()));

        Route saved = routeRepository.save(route);
        return new RouteResponse(saved, orsResult.geometry());
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getRoutes(Long tripId) {
        tripService.findTripById(tripId);
        return routeRepository.findByTripId(tripId).stream()
                .map(r -> new RouteResponse(r, deserializeGeometry(r.getGeometryJson())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse getRoute(Long tripId, Long routeId) {
        tripService.findTripById(tripId);
        Route route = routeRepository.findById(routeId)
                .filter(r -> r.getTrip().getId().equals(tripId))
                .orElseThrow(() -> new ResourceNotFoundException("找不到路線 ID: " + routeId));
        return new RouteResponse(route, deserializeGeometry(route.getGeometryJson()));
    }

    // ── 私有輔助方法 ──────────────────────────────────────────────

    private List<Attraction> resolveAttractions(Long tripId, List<Long> attractionIds) {
        List<Attraction> allTripAttractions = attractionRepository.findByTripIdOrderByDateAndTime(tripId);
        Map<Long, Attraction> attractionMap = allTripAttractions.stream()
                .collect(Collectors.toMap(Attraction::getId, a -> a));

        List<Attraction> result = new ArrayList<>();
        for (Long id : attractionIds) {
            Attraction attraction = attractionMap.get(id);
            if (attraction == null) {
                throw new InvalidInputException("景點 ID " + id + " 不屬於此行程或不存在");
            }
            result.add(attraction);
        }
        if (result.size() < 2) {
            throw new InvalidInputException("至少需要選擇兩個景點才能規劃路線");
        }
        return result;
    }

    private List<double[]> extractCoordinates(List<Attraction> attractions) {
        List<double[]> coords = new ArrayList<>();
        for (Attraction a : attractions) {
            if (a.getLatitude() == null || a.getLongitude() == null) {
                throw new InvalidInputException(
                        "景點「" + a.getName() + "」尚未取得座標，請確認地址是否正確");
            }
            coords.add(new double[]{a.getLatitude(), a.getLongitude()});
        }
        return coords;
    }

    private List<RouteSegmentResponse> buildSegments(List<Attraction> attractions,
                                                      OrsRouteResult orsResult,
                                                      TransportationMethod method) {
        List<RouteSegmentResponse> segments = new ArrayList<>();
        int segmentCount = attractions.size() - 1;
        if (segmentCount <= 0) return segments;

        // 將總時間和費用平均分配給每段（ORS 只回傳整體結果）
        int minutesPerSegment = (int) Math.round(orsResult.durationSeconds() / 60.0 / segmentCount);
        BigDecimal totalCost = calculateCost(method, orsResult.distanceMeters());
        BigDecimal costPerSegment = segmentCount > 0
                ? totalCost.divide(BigDecimal.valueOf(segmentCount), 0, RoundingMode.CEILING)
                : BigDecimal.ZERO;

        for (int i = 0; i < segmentCount; i++) {
            segments.add(new RouteSegmentResponse(
                    attractions.get(i).getName(),
                    attractions.get(i + 1).getName(),
                    minutesPerSegment,
                    costPerSegment
            ));
        }
        return segments;
    }

    /**
     * 依交通方式計算費用（台灣費率）
     */
    private BigDecimal calculateCost(TransportationMethod method, double distanceMeters) {
        double km = distanceMeters / 1000.0;
        double fare = switch (method) {
            case WALKING -> 0;
            case PUBLIC_TRANSIT -> 30; // 固定票價估算（ORS 不支援大眾運輸路線）
            case TAXI -> 85 + Math.max(0, (km - 1.25) / 0.2 * 5); // 起跳 85 元，每 200m +5 元
            case SELF_DRIVING -> (km / 12.0) * 30; // 油耗 12km/L，油價 30元/L
        };
        return BigDecimal.valueOf(Math.round(fare));
    }

    private String serializeGeometry(List<double[]> geometry) {
        try {
            return objectMapper.writeValueAsString(geometry);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<double[]> deserializeGeometry(String json) {
        if (json == null) return List.of();
        try {
            List<List<Double>> raw = objectMapper.readValue(json, List.class);
            return raw.stream()
                    .map(p -> new double[]{p.get(0), p.get(1)})
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
