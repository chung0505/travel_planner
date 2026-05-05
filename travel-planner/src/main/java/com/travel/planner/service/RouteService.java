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

        List<String> addresses = attractions.stream().map(Attraction::getAddress).toList();
        OrsRouteResult orsResult = orsService.getDirections(coordinates, addresses, method);

        List<RouteSegmentResponse> segments = buildSegments(attractions, orsResult, method);

        int totalMinutes = (int) Math.round(orsResult.durationSeconds() / 60.0);
        // 總費用從各段加總，確保使用 API 回傳的實際票價（transit）
        BigDecimal totalCost = segments.stream()
                .map(RouteSegmentResponse::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        List<String> addresses = attractions.stream().map(Attraction::getAddress).toList();
        OrsRouteResult orsResult = orsService.getDirections(coordinates, addresses, method);

        int totalMinutes = (int) Math.round(orsResult.durationSeconds() / 60.0);
        List<RouteSegmentResponse> confirmSegments = buildSegments(attractions, orsResult, method);
        BigDecimal totalCost = confirmSegments.stream()
                .map(RouteSegmentResponse::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Route route = new Route(trip, request.getAttractionIds(), method, totalMinutes, totalCost);
        route.confirm(serializeGeometry(orsResult.geometry()));

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

        List<double[]> legData = orsResult.legData();

        for (int i = 0; i < segmentCount; i++) {
            int minutes;
            BigDecimal cost;
            if (legData != null && !legData.isEmpty() && i < legData.size()) {
                minutes = (int) Math.round(legData.get(i)[1] / 60.0);
                // 優先使用 Google API 回傳的實際票價（transit 才有），否則自行估算
                List<Double> legFares = orsResult.legFares();
                if (legFares != null && i < legFares.size() && legFares.get(i) != null) {
                    cost = BigDecimal.valueOf(Math.round(legFares.get(i)));
                } else {
                    cost = Route.calculateCost(method, legData.get(i)[0]);
                }
            } else {
                minutes = (int) Math.round(orsResult.durationSeconds() / 60.0 / segmentCount);
                cost = Route.calculateCost(method, orsResult.distanceMeters())
                        .divide(BigDecimal.valueOf(segmentCount), 0, RoundingMode.CEILING);
            }
            // transit steps（捷運/公車換乘步驟）
            List<com.travel.planner.dto.response.TransitStepInfo> steps = null;
            if (orsResult.segmentSteps() != null && i < orsResult.segmentSteps().size()) {
                steps = orsResult.segmentSteps().get(i);
            }
            segments.add(new RouteSegmentResponse(
                    attractions.get(i).getName(),
                    attractions.get(i + 1).getName(),
                    minutes,
                    cost,
                    steps
            ));
        }
        return segments;
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
